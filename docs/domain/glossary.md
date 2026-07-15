# Glossário do domínio

Este glossário define a linguagem operacional e a relaciona com identificadores candidatos para uma futura implementação Java. Os nomes em inglês não representam classes já aprovadas ou implementadas.

As decisões confirmadas neste marco foram fornecidas por confirmação operacional e por um resumo de análise externa da planilha. O arquivo da planilha não esteve disponível nesta sessão e não foi inspecionado pelo Codex.

## Como ler as definições

- **Confirmado** identifica um fato ou regra sustentado pela evidência operacional disponível.
- **Candidato** identifica uma possível representação técnica para o Milestone 2B.
- **Aberto** identifica uma decisão ainda registrada em [Perguntas abertas](open-questions.md).

## Vocabulário

| Termo operacional | Identificador técnico candidato | Situação | Definição |
|---|---|---|---|
| Gás refrigerante | `RefrigerantGas` | Conceito confirmado; entidade candidata | Tipo de refrigerante associado aos cilindros. Informações técnicas ampliadas pertencem ao catálogo, não à atividade. |
| Nome operacional do gás | `operationalName` | Confirmado | Rótulo usado nos cilindros e na operação. Deve ser preservado sem padronização automática. |
| Cilindro | `Cylinder` | Conceito confirmado; entidade candidata | Recipiente físico não recarregável, identificado permanentemente pelo lacre e associado de forma imutável a um refrigerante. |
| Número do lacre | `sealNumber` | Confirmado | Identificador operacional único e permanente de um cilindro. Não muda durante seu ciclo de vida. |
| Atividade de uso | `UsageActivity` | Conceito confirmado; entidade candidata | Registro que começa com a saída e permanece o mesmo quando a pesagem de retorno é registrada. |
| Saída | — | Confirmado | Início normal da atividade, quando o peso bruto de saída é registrado e `startedAt` é produzido automaticamente. |
| Pesagem de retorno | — | Confirmado | Registro do peso bruto após o uso. A ausência desse peso não permite concluir se a atividade física ainda está ocorrendo. |
| Peso | `Weight` | Conceito confirmado; objeto de valor candidato | Medida em quilogramas, representada futuramente por `BigDecimal`, com resolução operacional observada de `0,01 kg`. |
| Peso bruto | — | Confirmado | Peso medido do cilindro físico somado ao refrigerante restante. Todo peso operacional é bruto. |
| Peso bruto de saída | `departureGrossWeight` | Confirmado | Peso bruto medido antes do uso. |
| Peso bruto de retorno | `returnGrossWeight` | Confirmado | Peso bruto medido depois do uso. Permanece ausente enquanto a pesagem de retorno não foi registrada. |
| Peso bruto inicial | `initialGrossWeight` | Confirmado | Primeira medição válida do cilindro recebido. Deve existir antes da primeira atividade, mas pode ser registrada junto com o cadastro ou depois dele. |
| Último peso bruto conhecido | `lastKnownGrossWeight` | Confirmado; fontes adicionais abertas | Medição válida mais recente obtida do peso inicial, de um retorno ou do peso final ao marcar o cilindro vazio. |
| Conteúdo líquido nominal | `nominalNetContent` | Conceito confirmado; atributo candidato | Quantidade nominal de refrigerante indicada no rótulo. Não representa peso bruto atual, tara ou peso vazio. |
| Peso bruto final | `finalGrossWeight` | Confirmado | Medição preservada quando o operador marca manualmente o cilindro como vazio. |
| Quantidade consumida | `consumedQuantity` | Confirmado | Valor derivado por `departureGrossWeight - returnGrossWeight`. O peso do mesmo cilindro se cancela na diferença. |
| Atividade aguardando pesagem de retorno | `ActivityStatus.AWAITING_RETURN_WEIGHT` | Estado confirmado; representação candidata | Atividade com saída e `startedAt`, sem `returnGrossWeight`, sem `completedAt` e sem consumo disponível. Texto de exibição: **Aguardando pesagem de retorno**. |
| Atividade concluída | `ActivityStatus.COMPLETED` | Estado confirmado; representação candidata | Atividade com ambos os pesos, consumo calculável e evidência temporal coerente com sua origem. No fluxo normal, possui `startedAt` e `completedAt`. |
| Cilindro ativo | `CylinderStatus.ACTIVE` | Estado candidato | Cilindro ainda disponível para uso, desde que também não possua atividade sem peso de retorno. |
| Cilindro vazio | `CylinderStatus.EMPTY` | Estado candidato | Cilindro marcado manualmente como sem refrigerante utilizável, preservado no histórico e impedido de iniciar novas atividades. |
| Cancelamento | `ActivityStatus.CANCELLED` ou operação de invalidação | Conceito candidato | Invalidação auditável que preserva os dados. A forma de representação permanece aberta. |
| Correção | — | Conceito confirmado; política aberta | Alteração controlada que não apaga silenciosamente o valor anterior. |
| Erro bloqueante | — | Confirmado | Violação que impede a operação e não pode ser superada por confirmação. |
| Alerta confirmável | — | Confirmado | Exceção legítima que permite continuar depois de confirmação consciente e registrável. |
| Instante de início | `startedAt` | Confirmado; representação candidata | Instante automático produzido ao registrar a saída normal. |
| Instante de conclusão | `completedAt` | Confirmado; representação candidata | Instante automático produzido ao registrar o peso bruto de retorno. |
| Instante de correção | `correctedAt` | Conceito confirmado; política aberta | Instante automático de cada correção futura, sem substituir eventos anteriores. |
| Instante de cancelamento | `cancelledAt` | Conceito confirmado; política aberta | Instante automático de eventual cancelamento ou invalidação. |
| Instante da marcação como vazio | `markedEmptyAt` | Confirmado; representação candidata | Instante automático da ação manual que muda o cilindro para `EMPTY`. |
| Data civil da atividade | — | Valor derivado | Data local derivada de `startedAt` usando `America/Sao_Paulo`; não é campo manual no fluxo normal. |
| Local da atividade | `activityLocation` | Atributo obrigatório | Texto livre não vazio, preservado exatamente como informado e sem ciclo de vida independente. |

## Nomes operacionais confirmados

- `R410A`
- `R32`
- `R-22`
- `R407C`
- `R134A`
- `R404`
- `141B`

`R404` não deve ser convertido automaticamente em `R404A`, e `141B` não deve ser convertido em `HCFC-141b`. Um catálogo técnico futuro pode adicionar descrições padronizadas sem substituir `operationalName`.

## Distinções essenciais

- `15,14` e `15,140` representam o mesmo valor numérico; zeros finais são formatação, não precisão física adicional.
- A resolução de `0,01 kg` não fixa a quantidade de casas exibidas pela interface e não autoriza arredondamento.
- Peso bruto, conteúdo líquido nominal e tara são conceitos diferentes; não existe tara ou peso vazio universal.
- Uma atividade aguardando pesagem e uma concluída são estados da mesma `UsageActivity`.
- A disponibilidade operacional combina o ciclo do cilindro com a ausência de atividade sem peso de retorno; não é um booleano independente.
- `Instant` é o tipo provável dos eventos automáticos. `America/Sao_Paulo` é usado apenas para exibição e derivação da data civil.
