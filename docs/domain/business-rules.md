# Regras de negócio

Este documento é a fonte canônica dos invariantes e das políticas de correção. A [Matriz de validação](validation-matrix.md) traduz essas regras em erros e alertas; o [Ciclo de vida](activity-lifecycle.md) detalha transições.

## Regras confirmadas

| ID | Regra |
|---|---|
| BR-C-01 | Uma atividade deve referenciar um cilindro existente. |
| BR-C-02 | O cadastro de um cilindro deve referenciar um gás refrigerante existente. |
| BR-C-03 | Pesos brutos informados não podem ser negativos. |
| BR-C-04 | `returnGrossWeight` não pode ser maior que `departureGrossWeight`. |
| BR-C-07 | Quando os dois pesos existem, `consumedQuantity = departureGrossWeight - returnGrossWeight`. |
| BR-C-08 | `consumedQuantity` é derivada e não deve ser editada independentemente dos pesos. |
| BR-C-09 | Uma confirmação nunca torna aceitável uma violação bloqueante. |
| BR-C-10 | `AWAITING_RETURN_WEIGHT` exige saída e `startedAt` presentes, retorno e `completedAt` ausentes e consumo indisponível. |
| BR-C-11 | `COMPLETED` exige ambos os pesos, evidência temporal coerente com a origem e consumo calculável; no fluxo normal, exige `startedAt` e `completedAt`. |
| BR-C-12 | Atividade aberta e concluída são estados da mesma `UsageActivity`. |
| BR-C-13 | Regras do domínio não podem existir somente em controllers, templates ou validação visual. |
| BR-C-14 | Alterações de peso que não representam consumo não podem ser registradas como atividades de consumo fictícias. |
| BR-C-15 | A saída normal exige cilindro, `departureGrossWeight` e `activityLocation` não vazio; a conclusão exige somente a atividade pendente e `returnGrossWeight`. |
| BR-C-16 | No fluxo normal, `startedAt` e `completedAt` são automáticos; `America/Sao_Paulo` define exibição e data civil derivada. |
| BR-C-17 | `sealNumber` identifica um único cilindro e permanece imutável. |
| BR-C-18 | Um cilindro não é recarregável e sua associação com `RefrigerantGas` é imutável. |
| BR-C-19 | Um `initialGrossWeight` válido deve existir antes da primeira atividade, sem criar `UsageActivity`. |
| BR-C-20 | Um cilindro não inicia nova atividade enquanto possuir outra sem `returnGrossWeight`. |
| BR-C-21 | Um cilindro marcado `EMPTY` preserva histórico e peso final, sai das seleções ativas e não inicia atividades. |
| BR-C-22 | O nome operacional do refrigerante deve ser preservado exatamente, sem renomeação automática. |
| BR-C-23 | `activityLocation` é obrigatório e preservado exatamente; ordem de serviço, técnico e observações continuam opcionais e sua ausência não gera alerta. |
| BR-C-24 | O gás histórico de uma atividade é obtido da associação imutável de seu cilindro. |
| BR-C-25 | `lastKnownGrossWeight` deriva da evidência cronológica válida mais recente entre peso inicial, retorno e peso final. |

## Regras substituídas pela evidência

| ID anterior | Situação no Milestone 2A.1 | Regra vigente |
|---|---|---|
| BR-C-05 | A redação anterior sobre local foi consolidada; a obrigatoriedade foi confirmada no Milestone 2C.3A. | BR-C-15 e BR-C-23 |
| BR-C-06 | Data operacional manual obrigatória foi substituída. | BR-C-16 |
| BR-R-01 | Limite de atividade aberta deixou de ser recomendação. | BR-C-20 |
| BR-R-03 | Disponibilidade somente derivada de atividades era insuficiente após confirmar `EMPTY`. | BR-C-20 e BR-C-21 |

## Regras recomendadas aguardando confirmação

| ID | Recomendação | Decisão relacionada |
|---|---|---|
| BR-R-02 | Sugerir `lastKnownGrossWeight` somente quando houver valor válido de origem aprovada, permitindo alteração com alerta quando a diferença for relevante. | [OQ-WGT-06](open-questions.md#oq-wgt-06) e [OQ-WGT-07](open-questions.md#oq-wgt-07) |
| BR-R-04 | Desencorajar exclusão física de atividade e preferir invalidação auditável. | [OQ-COR-03](open-questions.md#oq-cor-03) |
| BR-R-05 | Exigir motivo em correções materiais e cancelamentos, sem colocá-lo no formulário normal. | [OQ-COR-01](open-questions.md#oq-cor-01) e [OQ-COR-02](open-questions.md#oq-cor-02) |
| BR-R-06 | Não permitir reabertura de atividade concluída sem política aprovada. | [OQ-COR-01](open-questions.md#oq-cor-01) |

## Questões que impedem regras finais

- Valores incompatíveis com `0,01 kg` dependem da decisão entre rejeição e arredondamento em [OQ-WGT-09](open-questions.md#oq-wgt-09). Nenhum arredondamento silencioso é permitido.
- A capacidade máxima da balança permanece desconhecida em [OQ-WGT-10](open-questions.md#oq-wgt-10).
- Transferência, manutenção, recalibração e correção manual ainda podem ampliar as fontes de peso conhecido conforme [OQ-WGT-06](open-questions.md#oq-wgt-06).
- A tolerância de diferença do último peso e o limiar de consumo alto permanecem abertos em [OQ-WGT-07](open-questions.md#oq-wgt-07) e [OQ-WGT-08](open-questions.md#oq-wgt-08).
- A definição de duplicidade permanece aberta em [OQ-VAL-01](open-questions.md#oq-val-01).
- Datas ou instantes informados em importações e correções excepcionais permanecem abertos em [OQ-DAT-05](open-questions.md#oq-dat-05) e [OQ-DAT-06](open-questions.md#oq-dat-06).
- Marcar `EMPTY` durante uma atividade sem retorno depende de [OQ-CYL-06](open-questions.md#oq-cyl-06).

<a id="correcao-cancelamento-e-exclusao"></a>
## Correção, cancelamento e exclusão

### Confirmado

- Uma correção não pode apagar silenciosamente a informação anterior.
- O histórico necessário para compreender a atividade deve ser preservado.
- Uma combinação corrigida de campos deve continuar respeitando os invariantes.

### Recomendado, aguardando confirmação

- Atividade concluída é corrigida por operação explícita, com valores anteriores, valores novos, motivo e `correctedAt`.
- Cancelamento preserva o registro e o exclui dos cálculos normais.
- Exclusão física não faz parte da operação diária.

### Em aberto

- quais campos podem ser corrigidos;
- quando o motivo é obrigatório;
- se cancelamento pertence a `ActivityStatus` ou é uma invalidação separada;
- se alguma exigência legal permite ou obriga exclusão;
- como registrar autoria enquanto não existe autenticação;
- se uma atividade concluída pode ser reaberta.

## Rastreabilidade mínima requerida

A documentação do produto deve permitir futuramente preservar:

- `startedAt` da atividade normal;
- `completedAt` da atividade normal;
- valores anteriores e posteriores em correções;
- motivo quando exigido;
- cancelamento ou invalidação;
- `correctedAt`, `cancelledAt` e `markedEmptyAt` quando aplicáveis;
- confirmações de alertas relevantes.

Autoria por usuário permanece limitada pela ausência intencional de autenticação. A interface diária não deve exibir esses dados internos como campos permanentes.
