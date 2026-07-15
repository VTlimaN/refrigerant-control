# Ciclo de vida da atividade

Uma `UsageActivity` representa o fluxo completo iniciado na saída. O registro do retorno altera o estado da mesma atividade, sem criar uma entidade diferente.

## Estados candidatos

| Estado | Campos obrigatórios | Campos ausentes ou indisponíveis | Valor derivado |
|---|---|---|---|
| `AWAITING_RETURN_WEIGHT` | cilindro, `departureGrossWeight`, `activityLocation` e `startedAt` | `returnGrossWeight` e `completedAt` ausentes; `consumedQuantity` indisponível | nenhum consumo |
| `COMPLETED` | cilindro, `departureGrossWeight`, `activityLocation`, `returnGrossWeight` e evidência temporal coerente com a origem; no fluxo normal, `startedAt` e `completedAt` | nenhum peso operacional ausente | `consumedQuantity = departureGrossWeight - returnGrossWeight` |
| `CANCELLED` | dados anteriores e motivo, se o estado for aprovado | excluído dos cálculos normais | nenhum novo consumo válido |

O local da atividade é obrigatório e permanece inalterado durante todo o ciclo. Ordem de serviço, técnico e observações continuam opcionais em qualquer estado. Combinações contrárias à tabela são erros bloqueantes. `CANCELLED` continua candidato e pode ser substituído por invalidação auditável.

## Fluxo operacional normal

### Registrar saída

- **Origem:** atividade inexistente.
- **Precondições:** cilindro existente em `ACTIVE`, peso inicial válido antes do primeiro uso, nenhuma atividade do mesmo cilindro sem retorno, peso bruto de saída válido e local da atividade não vazio.
- **Efeito automático:** produzir `startedAt`.
- **Resultado:** `AWAITING_RETURN_WEIGHT`.
- **Valor derivado:** consumo indisponível.

### Registrar pesagem de retorno

- **Origem:** `AWAITING_RETURN_WEIGHT`.
- **Precondições:** peso bruto de retorno presente, não negativo e não superior ao peso bruto de saída.
- **Efeito automático:** produzir `completedAt`.
- **Resultado:** `COMPLETED` na mesma `UsageActivity`.
- **Valor derivado:** consumo calculável.

A ausência de `returnGrossWeight` é o único fato conhecido. Ela não demonstra se o serviço ainda ocorre ou se apenas falta pesar o cilindro.

## Operações excepcionais

### Criação diretamente como concluída

Não pertence ao fluxo normal. Pode existir futuramente somente para importação legada, correção histórica ou recuperação controlada de uma atividade não registrada na saída.

- **Precondições:** política excepcional aprovada, origem dos dados identificada, ambos os pesos válidos e tratamento temporal definido.
- **Resultado candidato:** `COMPLETED`.
- **Restrição:** não usar o instante de importação como se fosse o início real da atividade.

A restrição ao fluxo excepcional está respondida em [OQ-ACT-04](open-questions.md#oq-act-04). O tratamento temporal e de correção permanece aberto em [OQ-DAT-05](open-questions.md#oq-dat-05), [OQ-DAT-06](open-questions.md#oq-dat-06) e [OQ-COR-01](open-questions.md#oq-cor-01).

### Cancelamento ou correção

- Uma atividade em `AWAITING_RETURN_WEIGHT` ou `COMPLETED` só pode ser corrigida ou invalidada por política futura explícita.
- Valores anteriores e instantes de auditoria não podem desaparecer silenciosamente.
- `correctedAt` e `cancelledAt` são produzidos automaticamente quando essas operações forem aprovadas.

## Transições inválidas

- iniciar nova atividade para o mesmo cilindro enquanto outra não possui `returnGrossWeight`;
- iniciar atividade sem `activityLocation` não vazio;
- concluir sem `returnGrossWeight`;
- manter retorno, `completedAt` ou consumo em `AWAITING_RETURN_WEIGHT`;
- concluir quando o retorno supera a saída;
- registrar consumo independente dos pesos;
- reabrir atividade concluída sem política explícita;
- confirmar um alerta para contornar erro bloqueante.

## Instantes e data civil

`startedAt` e `completedAt` são automáticos no fluxo normal e provavelmente usam `Instant`. `America/Sao_Paulo` converte esses instantes para exibição e para a data civil derivada. Nenhuma data ou hora é digitada obrigatoriamente no fluxo normal.

`LocalDate` pode ser necessário somente em importação ou correção excepcional quando a fonte não contém instante completo. As classificações de valores passados ou futuros nesses fluxos permanecem em [Perguntas abertas](open-questions.md#datas-e-tempo).

## Decisões abertas

- cancelamento em `ActivityStatus` ou invalidação separada;
- mecanismo de correção e eventual reabertura;
- tratamento temporal de importações e correções históricas;
- marcação do cilindro como vazio enquanto existe atividade sem peso de retorno.

As políticas canônicas relacionadas estão em [Regras de negócio](business-rules.md#correcao-cancelamento-e-exclusao).
