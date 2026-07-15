# Escopo do MVP

O produto segue o princípio **“Completo por dentro, simples por fora.”** O fluxo diário deve exigir somente o que a operação precisa naquele momento, enquanto regras, estados e rastreabilidade protegem os dados internamente.

Este documento classifica capacidades do produto. O Milestone 2A.1 registra decisões; não implementa os itens classificados como MVP.

## Fluxo operacional normal

### Registrar saída

Informações visíveis obrigatórias:

1. cilindro ou número do lacre;
2. peso bruto de saída;
3. local da atividade.

O sistema registra `startedAt` automaticamente. O refrigerante vem da associação imutável do cilindro. O local é texto livre obrigatório e preservado exatamente. Ordem de serviço, técnico e observações podem aparecer em “Mais informações”, sem alerta quando ausentes.

### Registrar pesagem de retorno

Nova informação visível obrigatória:

1. peso bruto de retorno.

O sistema registra `completedAt`, calcula `consumedQuantity` e conclui a mesma atividade. A data civil exibida é derivada dos instantes usando `America/Sao_Paulo`.

Uma criação diretamente em `COMPLETED` não pertence ao fluxo normal. Ela fica restrita a futura importação legada, correção histórica ou recuperação controlada, sem inventar instantes do fato original.

## Classificação por área

| Área | MVP do produto | Futuro | Não justificado sem requisito confirmado |
|---|---|---|---|
| Atividade operacional | Registrar saída, aguardar pesagem, registrar retorno, calcular consumo, listar pendências e consultar histórico | Importação, correção histórica e recuperação controlada diretamente como concluída | Formulários independentes ou entidades diferentes para cada estado |
| Cilindro | `sealNumber`, refrigerante imutável, peso bruto inicial antes do primeiro uso, ciclo `ACTIVE`/`EMPTY` e marcação manual como vazio | Eventos adicionais de peso se confirmados | Fluxo separado para descarte físico ou disponibilidade booleana duplicada |
| Gás refrigerante | Preservar o nome operacional e permitir associação com cilindros | Catálogo técnico com fontes confiáveis | Renomear automaticamente rótulos operacionais ou exigir catálogo durante a atividade |
| Histórico | Pesquisa, consulta, identificação de atividades sem retorno e políticas futuras de correção/cancelamento | Auditoria com autoria após requisito de identidade | Exclusão física como operação normal |
| Proteção de dados | Backup e exportação conforme formatos a decidir | Importação com classificação manual de linhas ambíguas | Escolher formato ou infraestrutura sem consumidor confirmado |
| Automação | Instantes automáticos, consumo derivado e sugestão segura de `lastKnownGrossWeight` | Locais recentes e ordenação por frequência | Arredondamento, classificação de importação ou outra decisão irreversível automática |

Os [treze casos de uso](use-cases.md) descrevem essas capacidades sem antecipar telas ou infraestrutura.

## Separação dos dados

### Dados da atividade operacional

- cilindro;
- `departureGrossWeight`;
- `returnGrossWeight`, quando registrado;
- `startedAt` e `completedAt`;
- `ActivityStatus`;
- `activityLocation` obrigatório;
- ordem de serviço, técnico e observações opcionais;
- confirmações e rastreabilidade quando aplicáveis.

### Dados do cilindro

- `sealNumber` único e permanente;
- associação imutável com `RefrigerantGas`;
- `initialGrossWeight`, que deve existir antes da primeira atividade;
- `finalGrossWeight` e `markedEmptyAt` quando marcado vazio;
- `CylinderStatus` candidato;
- `nominalNetContent`, somente se houver necessidade de registrar o valor do rótulo.

O cadastro de lacre e refrigerante pode ocorrer antes da medição inicial. Registrar `initialGrossWeight` depois, mas antes do primeiro uso, não cria uma atividade fictícia nem exige uma entidade adicional.

### Dados do catálogo de refrigerantes

- `operationalName`, preservado exatamente;
- descrição e aplicações futuras;
- composição, segurança, cuidados e armazenamento futuros;
- referências técnicas confiáveis ainda abertas.

Os dados técnicos ficam fora do fluxo operacional.

### Funções administrativas

Backup, exportação, futura importação, usuários e configurações pertencem a uma área secundária. Backup e exportação fazem parte do MVP, mas seus formatos permanecem abertos. Autenticação, autorização e código de importação não pertencem a este marco.

## Informações adicionais

| Informação | Classificação | Regra |
|---|---|---|
| Local da atividade | Obrigatório | Texto livre não vazio, preservado exatamente e exigido no início da atividade. |
| Ordem de serviço | Opcional | Fica em divulgação progressiva. |
| Técnico | Opcional | Não representa autoria autenticada. |
| Observações | Opcional | Não gera alerta quando ausente. |
| Horários de saída e retorno | Automáticos | `startedAt` e `completedAt` não são digitados no fluxo normal. |
| Motivo de correção ou cancelamento | Excepcional | Só aparece quando a política futura exigir. |
| Detalhes adicionais | Não justificado | Não define necessidade verificável. |

## Ciclo mínimo do cilindro

- `ACTIVE`: pode participar de nova atividade somente se possuir peso inicial e nenhuma atividade sem retorno.
- `EMPTY`: preserva histórico, peso bruto final e instante da marcação; não aparece nas seleções ativas nem inicia novas atividades.

Não existe limite automático de peso vazio. A marcação é manual, e o software não modela o descarte físico.

## Requisito de migração futura

Na planilha antiga, uma linha com saída e sem retorno pode ser uma atividade sem pesagem de retorno ou o peso inicial de um cilindro recém-recebido. A importação deve exigir classificação humana explícita e manter `initialGrossWeight` separado de `UsageActivity`.

## Fora do Milestone 2A.1

O marco não cria classes, enums, telas, APIs, persistência, segurança, importador, backup ou exportação. A separação da futura implementação Java permanece registrada no [ADR de abordagem da modelagem](../architecture/adr-001-domain-modeling-approach.md).
