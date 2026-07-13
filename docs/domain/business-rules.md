# Regras de negócio

Este documento é a fonte canônica dos invariantes e das políticas de correção. A [Matriz de validação](validation-matrix.md) traduz essas regras em erros e alertas; o [Ciclo de vida](activity-lifecycle.md) detalha transições.

## Regras confirmadas

| ID | Regra |
|---|---|
| BR-C-01 | Uma atividade deve referenciar um cilindro existente. |
| BR-C-02 | O cadastro de um cilindro deve referenciar um gás refrigerante existente. |
| BR-C-03 | Pesos informados não podem ser negativos. |
| BR-C-04 | O peso de retorno não pode ser maior que o peso de saída. |
| BR-C-05 | Toda atividade deve possuir local. |
| BR-C-06 | Toda atividade deve possuir a data operacional mínima definida para seu fluxo. |
| BR-C-07 | Quando os dois pesos existem, `consumedQuantity = departureWeight - returnWeight`. |
| BR-C-08 | `consumedQuantity` é derivada e não deve ser editada independentemente dos pesos. |
| BR-C-09 | Uma confirmação nunca torna aceitável uma violação bloqueante. |
| BR-C-10 | `AWAITING_RETURN` exige saída presente, retorno ausente e consumo indisponível. |
| BR-C-11 | `COMPLETED` exige saída e retorno presentes e consumo calculável. |
| BR-C-12 | Atividade aberta e concluída são estados da mesma `UsageActivity`. |
| BR-C-13 | Regras do domínio não podem existir somente em controllers, templates ou validação visual. |
| BR-C-14 | Alterações de peso que não representam consumo não podem ser registradas como atividades de consumo fictícias. |

## Regras recomendadas aguardando confirmação

| ID | Recomendação | Decisão relacionada |
|---|---|---|
| BR-R-01 | Permitir no máximo uma atividade aberta incompatível por cilindro. | [OQ-ACT-01](open-questions.md#oq-act-01) |
| BR-R-02 | Sugerir o último peso conhecido como saída, permitindo alteração com alerta quando a diferença for relevante. | [OQ-WGT-07](open-questions.md#oq-wgt-07) |
| BR-R-03 | Derivar a disponibilidade do cilindro das atividades abertas, sem campo duplicado. | [OQ-ACT-01](open-questions.md#oq-act-01) |
| BR-R-04 | Desencorajar exclusão física de atividade e preferir invalidação auditável. | [OQ-COR-03](open-questions.md#oq-cor-03) |
| BR-R-05 | Exigir motivo em correções materiais e cancelamentos, sem colocá-lo no formulário normal. | [OQ-COR-01](open-questions.md#oq-cor-01) e [OQ-COR-02](open-questions.md#oq-cor-02) |
| BR-R-06 | Não permitir reabertura de atividade concluída sem política aprovada. | [OQ-COR-01](open-questions.md#oq-cor-01) |

## Questões que impedem regras finais

- A atribuição histórica do gás permanece aberta em [OQ-CYL-04](open-questions.md#oq-cyl-04). Portanto, não é regra final derivar o gás histórico da associação atual do cilindro.
- A origem do último peso conhecido depende de peso inicial e eventos fora do consumo, tratados em [OQ-WGT-05](open-questions.md#oq-wgt-05) e [OQ-WGT-06](open-questions.md#oq-wgt-06).
- Unidade, precisão, tara, tolerâncias e limites não possuem regras até obtenção de evidência.
- A definição de duplicidade e de consumo excepcionalmente alto permanece aberta.
- Datas diferentes de saída e retorno, datas futuras e alerta de data passada ainda dependem de decisão.

<a id="correcao-cancelamento-e-exclusao"></a>
## Correção, cancelamento e exclusão

### Confirmado

- Uma correção não pode apagar silenciosamente a informação anterior.
- O histórico necessário para compreender a atividade deve ser preservado.
- Uma combinação corrigida de campos deve continuar respeitando os invariantes.

### Recomendado, aguardando confirmação

- Atividade concluída é corrigida por operação explícita, com valores anteriores, valores novos, motivo e instante técnico.
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

- criação da atividade;
- conclusão da atividade;
- valores anteriores e posteriores em correções;
- motivo quando exigido;
- cancelamento ou invalidação;
- instantes técnicos correspondentes;
- confirmações de alertas relevantes.

Autoria por usuário permanece limitada pela ausência intencional de autenticação. A interface diária não deve exibir esses dados internos como campos permanentes.
