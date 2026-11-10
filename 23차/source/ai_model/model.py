"""무작위 초기화 소형 encoder-decoder와 학습 입력 pointer입니다."""
from dataclasses import asdict, dataclass
import math
import torch
from torch import nn


@dataclass
class ModelConfig:
    input_vocab: int
    output_vocab: int
    dim: int = 192
    heads: int = 4
    layers: int = 2
    ff: int = 768
    dropout: float = 0.1
    max_input: int = 1024
    max_output: int = 512

    def to_dict(self):
        return asdict(self)


class Attention(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.heads = config.heads
        self.head_dim = config.dim // config.heads
        if config.dim % config.heads:
            raise ValueError("attention 차원은 head 수로 나누어져야 합니다")
        self.q = nn.Linear(config.dim, config.dim)
        self.k = nn.Linear(config.dim, config.dim)
        self.v = nn.Linear(config.dim, config.dim)
        self.out = nn.Linear(config.dim, config.dim)
        self.dropout = nn.Dropout(config.dropout)

    def forward(self, query, memory, keep, causal=False):
        batch, qlen, dim = query.shape
        mlen = memory.shape[1]
        q = self.q(query).reshape(batch, qlen, self.heads, self.head_dim).transpose(1, 2)
        k = self.k(memory).reshape(batch, mlen, self.heads, self.head_dim).transpose(1, 2)
        v = self.v(memory).reshape(batch, mlen, self.heads, self.head_dim).transpose(1, 2)
        scores = torch.matmul(q, k.transpose(-1, -2)) / math.sqrt(self.head_dim)
        scores = scores.masked_fill(~keep[:, None, None, :], -1e30)
        if causal:
            mask = torch.arange(mlen, device=query.device)[None, :] > torch.arange(qlen, device=query.device)[:, None]
            scores = scores.masked_fill(mask[None, None, :, :], -1e30)
        result = torch.matmul(self.dropout(scores.softmax(-1)), v)
        return self.out(result.transpose(1, 2).reshape(batch, qlen, dim))


class EncoderLayer(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.attention = Attention(config)
        self.norm1 = nn.LayerNorm(config.dim)
        self.norm2 = nn.LayerNorm(config.dim)
        self.ff = nn.Sequential(nn.Linear(config.dim, config.ff), nn.GELU(), nn.Dropout(config.dropout), nn.Linear(config.ff, config.dim))
        self.dropout = nn.Dropout(config.dropout)

    def forward(self, value, keep):
        normalized = self.norm1(value)
        value = value + self.dropout(self.attention(normalized, normalized, keep))
        return value + self.dropout(self.ff(self.norm2(value)))


class DecoderLayer(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.self_attention = Attention(config)
        self.cross_attention = Attention(config)
        self.norm1, self.norm2, self.norm3 = (nn.LayerNorm(config.dim) for _ in range(3))
        self.ff = nn.Sequential(nn.Linear(config.dim, config.ff), nn.GELU(), nn.Dropout(config.dropout), nn.Linear(config.ff, config.dim))
        self.dropout = nn.Dropout(config.dropout)

    def forward(self, value, memory, target_keep, source_keep):
        normalized = self.norm1(value)
        value = value + self.dropout(self.self_attention(normalized, normalized, target_keep, causal=True))
        value = value + self.dropout(self.cross_attention(self.norm2(value), memory, source_keep))
        return value + self.dropout(self.ff(self.norm3(value)))


class PlanTransformer(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.config = config
        self.source_embedding = nn.Embedding(config.input_vocab, config.dim, padding_idx=0)
        self.target_embedding = nn.Embedding(config.output_vocab, config.dim, padding_idx=0)
        self.source_position = nn.Embedding(config.max_input, config.dim)
        self.target_position = nn.Embedding(config.max_output, config.dim)
        self.encoder = nn.ModuleList(EncoderLayer(config) for _ in range(config.layers))
        self.decoder = nn.ModuleList(DecoderLayer(config) for _ in range(config.layers))
        self.encoder_norm = nn.LayerNorm(config.dim)
        self.decoder_norm = nn.LayerNorm(config.dim)
        self.output = nn.Linear(config.dim, config.output_vocab)
        self.start_query, self.end_query = nn.Linear(config.dim, config.dim), nn.Linear(config.dim, config.dim)
        self.start_key, self.end_key = nn.Linear(config.dim, config.dim), nn.Linear(config.dim, config.dim)

    def encode(self, source, source_keep):
        value = self.source_embedding(source) * math.sqrt(self.config.dim)
        value = value + self.source_position(torch.arange(source.shape[1], device=source.device))[None, :, :]
        for layer in self.encoder:
            value = layer(value, source_keep)
        return self.encoder_norm(value)

    def decode(self, memory, target, source_keep, copy_keep):
        value = self.target_embedding(target) * math.sqrt(self.config.dim)
        value = value + self.target_position(torch.arange(target.shape[1], device=target.device))[None, :, :]
        for layer in self.decoder:
            value = layer(value, memory, target.ne(0), source_keep)
        value = self.decoder_norm(value)
        starts = torch.matmul(self.start_query(value), self.start_key(memory).transpose(-1, -2)) / math.sqrt(self.config.dim)
        ends = torch.matmul(self.end_query(value), self.end_key(memory).transpose(-1, -2)) / math.sqrt(self.config.dim)
        starts = starts.masked_fill(~copy_keep[:, None, :], -1e30)
        ends = ends.masked_fill(~copy_keep[:, None, :], -1e30)
        return self.output(value), starts, ends

    def forward(self, source, target, source_keep, copy_keep):
        return self.decode(self.encode(source, source_keep), target, source_keep, copy_keep)
