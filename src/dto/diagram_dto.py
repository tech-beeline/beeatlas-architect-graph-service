from pydantic import BaseModel
from typing import List, Optional

class ComponentDTO(BaseModel):
    """Объект, описывающий системный компонент."""
    softwaresystem: str
    container: str
    component: str

class SequenceItemDTO(BaseModel):
    """Элемент последовательности."""
    method: str
    tcCode: str
    out: ComponentDTO
    in_: ComponentDTO  # `in` — зарезервированное слово, поэтому использовано `in_`
    order: int

class DiagramDTO(BaseModel):
    """Корневой DTO, содержащий ключ диаграммы и массив последовательностей."""
    diagramKey: str
    sequence: List[SequenceItemDTO]