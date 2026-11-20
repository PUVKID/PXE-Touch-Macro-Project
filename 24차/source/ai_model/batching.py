"""입력·학습 정답의 offset과 패딩을 구성합니다."""
import torch
from .schema import TOKEN_TO_ID
from .literal_codec import LiteralSlotCodec
from .codec_contract import input_contract


class NeuralBatch(list):
    """기존7개 tensor와 loss 전용 mask를 분리합니다. mask/gold는 encoder feature가 아닙니다."""

    pointer_start_keep = None
    pointer_end_keep = None
    input_contract = None


def encode_row(row, tokenizer, max_input=1024, max_output=512, *, byte_probability=0.0, augmentation_rng=None, augmentation_stats=None):
    if byte_probability or augmentation_rng is not None or augmentation_stats is not None:
        encoded = tokenizer.encode_request_training(row["command"], row["context"], byte_probability=byte_probability, rng=augmentation_rng, max_input=max_input, stats=augmentation_stats)
    else:
        encoded = tokenizer.encode_request(row["command"], row["context"])
    if not 0 < len(encoded["ids"]) <= max_input or not 1 <= len(row["tokens"]) < max_output:
        raise ValueError("문맥 상한 초과: 자료를 잘라 학습하지 않습니다")
    ids, starts, ends = [], [], []
    for token in row["tokens"]:
        if isinstance(token, dict):
            ids.append(TOKEN_TO_ID["COPY"])
            start, end = tokenizer.span_to_tokens(encoded, token["start"], token["end"])
            starts.append(start); ends.append(end)
        else:
            ids.append(TOKEN_TO_ID[token]); starts.append(-100); ends.append(-100)
    result = {"source": encoded["ids"], "command_tokens": encoded["command_tokens"], "target": ids, "starts": starts, "ends": ends}
    if isinstance(tokenizer, LiteralSlotCodec):
        result["input_contract"] = input_contract(tokenizer)
        result["pointer_start_keep"] = encoded["start_keep"]
        result["pointer_end_keep"] = [tokenizer.end_mask(encoded, start) if start != -100 else [False] * len(encoded["ids"]) for start in starts]
        result["copy_keep"] = [a or b for a, b in zip(encoded["start_keep"], encoded["end_keep"])]
    return result


def collate(rows, device):
    contracts = [row.get("input_contract") for row in rows]
    if not rows or any(contract != contracts[0] for contract in contracts):
        raise ValueError("빈 batch 또는 서로 다른 codec batch 혼합")
    slen, tlen = max(len(r["source"]) for r in rows), max(len(r["target"]) for r in rows)
    source = torch.zeros((len(rows), slen), dtype=torch.long)
    target = torch.zeros((len(rows), tlen), dtype=torch.long)
    starts = torch.full_like(target, -100); ends = torch.full_like(target, -100)
    copy_keep = torch.zeros_like(source, dtype=torch.bool)
    pointer_start = torch.zeros_like(source, dtype=torch.bool) if contracts[0] is not None else None
    pointer_end = torch.zeros((len(rows), tlen, slen), dtype=torch.bool) if contracts[0] is not None else None
    for i, row in enumerate(rows):
        source[i, :len(row["source"])] = torch.tensor(row["source"])
        target[i, :len(row["target"])] = torch.tensor(row["target"])
        starts[i, :len(row["starts"])] = torch.tensor(row["starts"])
        ends[i, :len(row["ends"])] = torch.tensor(row["ends"])
        if pointer_start is None:
            copy_keep[i, :row["command_tokens"]] = True
        else:
            copy_keep[i, :len(row["source"])] = torch.tensor(row["copy_keep"])
            pointer_start[i, :len(row["source"])] = torch.tensor(row["pointer_start_keep"])
            pointer_end[i, :len(row["target"]), :len(row["source"])] = torch.tensor(row["pointer_end_keep"])
    prefix = torch.cat((torch.full((len(rows), 1), TOKEN_TO_ID["BOS"], dtype=torch.long), target[:, :-1]), dim=1)
    batch = NeuralBatch(value.to(device) for value in (source, prefix, source.ne(0), copy_keep, target, starts, ends))
    if pointer_start is not None:
        batch.pointer_start_keep, batch.pointer_end_keep = pointer_start.to(device), pointer_end.to(device)
        batch.input_contract = contracts[0]
    return batch


def pointer_logits_for_batch(model, batch):
    source, prefix, source_keep, copy_keep, target, starts, ends = batch
    if getattr(batch, "input_contract", None) is not None and getattr(model, "input_contract", None) != batch.input_contract:
        raise ValueError("loss의 모델과 codec fingerprint 불일치")
    logits, start_logits, end_logits = model(source, prefix, source_keep, copy_keep)
    if getattr(batch, "pointer_start_keep", None) is not None:
        start_logits = start_logits.masked_fill(~batch.pointer_start_keep[:, None, :], -1e30)
        end_logits = end_logits.masked_fill(~batch.pointer_end_keep, -1e30)
    return logits, start_logits, end_logits


def loss_for_batch(model, batch):
    source, prefix, source_keep, copy_keep, target, starts, ends = batch
    logits, start_logits, end_logits = pointer_logits_for_batch(model, batch)
    token_loss = torch.nn.functional.cross_entropy(logits.flatten(0, 1), target.flatten(), ignore_index=0)
    has_copy = bool(starts.ne(-100).any())
    if has_copy:
        start_loss = torch.nn.functional.cross_entropy(start_logits.flatten(0, 1), starts.flatten(), ignore_index=-100)
        end_loss = torch.nn.functional.cross_entropy(end_logits.flatten(0, 1), ends.flatten(), ignore_index=-100)
    else:
        start_loss = logits.sum()*0; end_loss = logits.sum()*0
    return token_loss + 0.5*(start_loss+end_loss), (token_loss.detach(), start_loss.detach(), end_loss.detach())
