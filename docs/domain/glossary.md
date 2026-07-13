# Glossário do domínio

Este glossário define a linguagem usada na operação e a relaciona com possíveis identificadores de uma futura implementação Java. Os nomes em inglês são candidatos técnicos, não classes ou decisões de implementação deste marco.

## Como ler as definições

- **Confirmado** indica um conceito necessário para descrever a operação já conhecida.
- **Candidato** indica uma forma possível de representar o conceito no futuro, ainda sujeita à modelagem e às decisões registradas em [Perguntas abertas](open-questions.md).

## Vocabulário

| Termo operacional | Identificador técnico candidato | Situação | Definição |
|---|---|---|---|
| Gás refrigerante | `RefrigerantGas` | Conceito confirmado; entidade candidata | Tipo de fluido refrigerante. Seus dados técnicos pertencem ao catálogo e não ao registro da atividade. |
| Cilindro | `Cylinder` | Conceito confirmado; entidade candidata | Recipiente identificado usado na operação e associado a um gás refrigerante segundo uma política histórica ainda não definida. |
| Atividade de uso | `UsageActivity` | Conceito confirmado; entidade candidata | Registro operacional único que começa com uma saída e pode ser criado já concluído ou permanecer aguardando retorno. |
| Saída | — | Confirmado | Momento operacional em que o cilindro parte para uma atividade, com seu peso de saída registrado. |
| Retorno | — | Confirmado | Retorno do cilindro após a atividade, quando o peso de retorno se torna conhecido. Não é um conceito separado de atividade. |
| Peso | `Weight` | Conceito confirmado; objeto de valor candidato | Medida de massa usada na operação. Unidade, precisão, tara e tolerâncias ainda dependem de decisão humana. |
| Peso de saída | `departureWeight` | Confirmado | Peso medido antes da atividade. É obrigatório tanto para uma atividade aberta quanto para uma concluída. |
| Peso de retorno | `returnWeight` | Confirmado | Peso medido após a atividade. Fica ausente enquanto a atividade aguarda retorno. |
| Quantidade consumida | `consumedQuantity` | Confirmado | Valor calculado por `departureWeight - returnWeight`. Só fica disponível quando os dois pesos existem. |
| Atividade aguardando retorno | `ActivityStatus.AWAITING_RETURN` | Estado confirmado; representação candidata | Atividade com saída registrada, peso de retorno ausente e consumo indisponível. |
| Atividade concluída | `ActivityStatus.COMPLETED` | Estado confirmado; representação candidata | Atividade com pesos de saída e retorno presentes e consumo calculável. |
| Cancelamento | `ActivityStatus.CANCELLED` ou operação de invalidação | Conceito candidato | Invalidação auditável que preserva os dados históricos. Ainda será decidido se integra `ActivityStatus` ou se é uma operação separada. |
| Correção | — | Conceito confirmado; política pendente | Alteração controlada de informação já registrada, sem apagar silenciosamente o valor anterior. |
| Erro bloqueante | — | Confirmado | Violação que impede a continuidade porque deixaria o domínio inválido. Não pode ser ignorada por confirmação. |
| Alerta confirmável | — | Confirmado | Situação excepcional que permite continuar depois de uma confirmação consciente e registrável. |
| Último peso conhecido | `lastKnownWeight` | Confirmado; origem pendente | Peso válido mais recente conhecido para o cilindro. Pode vir de um retorno ou de outro evento operacional legítimo ainda não modelado. |
| Data operacional | — | Confirmado | Data civil informada para um fato da operação, provavelmente representada por `LocalDate`. Não se confunde com horário técnico de auditoria. |
| Instante de auditoria | — | Confirmado | Momento técnico registrado automaticamente para criação, conclusão, correção ou cancelamento, provavelmente representado por `Instant`. |
| Local da atividade | `activityLocation` | Atributo confirmado | Texto que identifica onde a atividade ocorreu. Não possui ciclo de vida independente confirmado. |
| Estado da atividade | `ActivityStatus` | Conceito confirmado; enumeração candidata | Representação explícita da fase do ciclo de vida, cuja consistência com os campos é obrigatória. |

## Distinções essenciais

- A linguagem operacional é usada nos documentos e na futura interface em português brasileiro.
- Os identificadores em inglês servem apenas como vocabulário técnico para o futuro Milestone 2B.
- Uma atividade aberta e uma atividade concluída são estados da mesma atividade, não conceitos `OpenActivity` e `CompletedActivity`.
- “Último peso conhecido” é mais amplo que “último peso de retorno”: recarga, manutenção ou correção podem alterar o peso sem representar consumo.
- Data operacional é informada no contexto da operação; instante de auditoria é produzido internamente para rastreabilidade.
