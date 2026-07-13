# Ciclo de vida da atividade

Uma `UsageActivity` representa todo o fluxo de saída e retorno. Os estados não criam entidades diferentes.

## Estados candidatos

| Estado | Campos obrigatórios | Campos ausentes ou indisponíveis | Valor derivado |
|---|---|---|---|
| `AWAITING_RETURN` | cilindro, `departureWeight`, local e data de saída | `returnWeight` ausente; `consumedQuantity` indisponível | nenhum consumo |
| `COMPLETED` | cilindro, `departureWeight`, `returnWeight`, local e datas operacionais aprovadas | nenhum peso pode estar ausente | `consumedQuantity = departureWeight - returnWeight` |
| `CANCELLED` | dados históricos anteriores e motivo de cancelamento, se o estado for aprovado | excluído dos cálculos normais | nenhum novo consumo válido |

Qualquer combinação contrária à tabela é um erro bloqueante do domínio. `CANCELLED` ainda não é decisão final: pode ser substituído por uma operação auditável de invalidação.

## Transições candidatas

### Criação diretamente como concluída

- **Origem:** atividade ainda não registrada.
- **Precondições:** cilindro existente; ausência de conflito aberto conforme política a aprovar; pesos, local e data válidos; alertas confirmados quando aplicáveis.
- **Resultado:** `COMPLETED`.
- **Efeito:** consumo torna-se calculável.

### Criação aguardando retorno

- **Origem:** atividade ainda não registrada.
- **Precondições:** cilindro existente; ausência de conflito aberto conforme política a aprovar; saída, local e data válidos; retorno não informado.
- **Resultado:** `AWAITING_RETURN`.
- **Efeito:** consumo permanece indisponível.

### Conclusão de atividade aberta

- **Origem:** `AWAITING_RETURN`.
- **Precondições:** retorno presente, não negativo e não superior à saída; data de retorno válida caso venha a ser exigida; alertas confirmados quando aplicáveis.
- **Resultado:** `COMPLETED` na mesma `UsageActivity`.
- **Efeito:** consumo passa a ser calculável.

### Cancelamento de atividade aberta

- **Origem:** `AWAITING_RETURN`.
- **Precondições:** política de cancelamento aprovada e motivo informado.
- **Resultado:** `CANCELLED` ou registro de invalidação, conforme decisão futura.
- **Efeito:** informações anteriores permanecem preservadas e a atividade não participa do consumo normal.

### Correção ou invalidação de atividade concluída

- **Origem:** `COMPLETED`.
- **Precondições:** política aprovada, motivo quando obrigatório e novos valores válidos.
- **Resultado:** não definido até a escolha da política de correção e cancelamento.
- **Efeito necessário:** valores anteriores e instantes de auditoria não podem desaparecer silenciosamente.

## Transições inválidas

- concluir sem retorno;
- manter retorno ou consumo em `AWAITING_RETURN`;
- concluir quando o retorno supera a saída;
- registrar consumo independente dos pesos;
- mudar uma atividade concluída para aberta sem política explícita;
- alterar ou retirar cancelamento sem política aprovada;
- confirmar um alerta para contornar erro bloqueante.

## Datas e instantes

A data de saída é informação operacional. A necessidade de uma data de retorno informada pela pessoa permanece aberta. Criação, conclusão, correção e cancelamento podem produzir instantes técnicos automáticos, sem ocupar o formulário normal.

As decisões sobre datas diferentes, fuso, futuro e datas passadas estão em [Perguntas abertas](open-questions.md#datas-e-tempo).

## Decisões abertas

- cancelamento em `ActivityStatus` ou operação de invalidação;
- possibilidade e mecanismo de correção de concluída;
- proibição definitiva de reabertura;
- data de retorno manual ou somente instante automático;
- tratamento de atividade aberta quando o cilindro é desativado ou substituído.

As políticas canônicas relacionadas estão em [Regras de negócio](business-rules.md#correcao-cancelamento-e-exclusao).
