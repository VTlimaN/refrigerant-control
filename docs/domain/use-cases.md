# Casos de uso

Os treze casos de uso descrevem objetivos operacionais sem definir telas, rotas, tabelas ou frameworks. Falhas e alertas classificados usam os IDs da [Matriz de validação](validation-matrix.md), fonte canônica dessas condições. Situações ainda não aprovadas remetem às [Perguntas abertas](open-questions.md) e não são tratadas como validações confirmadas.

## UC-01 — Cadastrar gás refrigerante

- **Objetivo:** disponibilizar um refrigerante para associação imutável com cilindros.
- **Informação mínima:** `operationalName` preservado como usado na operação.
- **Precondições:** rótulo operacional identificado; campos técnicos ampliados não são exigidos.
- **Fluxo principal:** informar o nome operacional, validar e registrar o gás sem renomeá-lo automaticamente.
- **Fluxo alternativo:** adicionar informações técnicas futuras somente quando houver fonte confiável.
- **Falhas bloqueantes:** nenhuma classificação adicional confirmada; critérios técnicos ampliados permanecem em [OQ-CAT-01](open-questions.md#oq-cat-01).
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** gás disponível para cilindros, mantendo um dos rótulos operacionais confirmados em [OQ-CAT-02](open-questions.md#oq-cat-02).

## UC-02 — Cadastrar cilindro

- **Objetivo:** registrar um recipiente físico por seu lacre permanente.
- **Informação mínima:** `sealNumber` único e refrigerante existente.
- **Precondições:** gás cadastrado e lacre ainda não utilizado.
- **Fluxo principal:** informar lacre e refrigerante, criar o cilindro em `ACTIVE` e tornar a associação imutável.
- **Fluxo alternativo:** registrar `initialGrossWeight` na mesma operação ou depois, obrigatoriamente antes da primeira atividade.
- **Falhas bloqueantes:** informação essencial ausente (`VAL-B-03`), gás inexistente (`VAL-B-08`) ou lacre duplicado (`VAL-B-09`).
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** cilindro cadastrado; enquanto não possuir peso bruto inicial, não pode começar sua primeira atividade.

## UC-03 — Registrar atividade concluída excepcionalmente

- **Objetivo:** recuperar um fato histórico por importação legada, correção ou operação controlada quando a saída não foi registrada no momento correto.
- **Informação mínima:** cilindro, ambos os pesos brutos, origem dos dados e informação temporal exigida pela futura política excepcional.
- **Precondições:** operação excepcional aprovada conforme [OQ-ACT-04](open-questions.md#oq-act-04), classificação humana da origem e regras temporais e de correção definidas.
- **Fluxo principal:** identificar a exceção, validar os dados, preservar sua origem, calcular o consumo e registrar a atividade como `COMPLETED`.
- **Fluxo alternativo:** interromper o registro quando não for possível distinguir peso inicial de atividade ou estabelecer informações temporais confiáveis.
- **Falhas bloqueantes:** pesos ou referências inválidos (`VAL-B-01` a `VAL-B-04`), conflito (`VAL-B-05`), transição inválida (`VAL-B-06`) ou estado incoerente (`VAL-B-07`).
- **Alertas confirmáveis:** somente os alertas cuja condição já esteja aprovada; políticas temporais e de correção permanecem em [OQ-DAT-05](open-questions.md#oq-dat-05), [OQ-DAT-06](open-questions.md#oq-dat-06) e [OQ-COR-01](open-questions.md#oq-cor-01).
- **Resultado:** uma atividade histórica controlada em `COMPLETED`, sem usar o instante de registro como início real inventado.

## UC-04 — Registrar saída aguardando pesagem de retorno

- **Objetivo:** iniciar o fluxo normal antes de existir peso bruto de retorno.
- **Informação mínima:** cilindro ou lacre e `departureGrossWeight`.
- **Precondições:** cilindro existente em `ACTIVE`, `initialGrossWeight` válido antes do primeiro uso e nenhuma atividade do cilindro sem peso de retorno.
- **Fluxo principal:** selecionar o cilindro; sugerir `lastKnownGrossWeight` somente quando existir valor válido com origem cronológica aprovada em [OQ-WGT-05](open-questions.md#oq-wgt-05) e [OQ-WGT-06](open-questions.md#oq-wgt-06); permitir a informação do peso bruto; registrar `startedAt` automaticamente e iniciar a atividade.
- **Fluxo alternativo:** informar diretamente o peso quando não houver sugestão válida ou confirmar uma diferença legítima quando a tolerância futura assim exigir.
- **Falhas bloqueantes:** peso negativo (`VAL-B-01`), informação essencial ausente (`VAL-B-03`), cilindro inexistente (`VAL-B-04`), atividade anterior sem retorno (`VAL-B-05`) ou cilindro vazio (`VAL-B-11`).
- **Alertas confirmáveis:** diferença do último peso (`VAL-W-02`) e possível duplicidade (`VAL-W-05`) quando seus critérios forem aprovados.
- **Resultado:** uma `UsageActivity` em `AWAITING_RETURN_WEIGHT`, sem retorno, `completedAt` ou consumo.

## UC-05 — Registrar pesagem de retorno

- **Objetivo:** registrar o retorno e concluir a mesma atividade iniciada na saída.
- **Informação mínima:** atividade em andamento e `returnGrossWeight`.
- **Precondições:** atividade em `AWAITING_RETURN_WEIGHT`.
- **Fluxo principal:** localizar a atividade, informar o peso bruto de retorno, validar, registrar `completedAt`, calcular o consumo e concluir.
- **Fluxo alternativo:** confirmar consumo zero ou alto quando a situação for legítima e o critério aplicável estiver aprovado.
- **Falhas bloqueantes:** peso negativo (`VAL-B-01`), retorno maior que saída (`VAL-B-02`), transição inválida (`VAL-B-06`) ou estado incoerente (`VAL-B-07`).
- **Alertas confirmáveis:** consumo zero (`VAL-W-01`) e consumo alto (`VAL-W-03`).
- **Resultado:** a atividade existente passa a `COMPLETED`; nenhuma data manual ou segunda atividade é criada.

## UC-06 — Listar atividades aguardando pesagem de retorno

- **Objetivo:** identificar atividades cujo peso bruto de retorno ainda não foi registrado.
- **Informação mínima:** nenhuma entrada obrigatória; filtros futuros permanecem opcionais.
- **Precondições:** existência de atividades registradas.
- **Fluxo principal:** consultar atividades coerentes com `AWAITING_RETURN_WEIGHT`.
- **Fluxo alternativo:** aplicar critérios de ordenação ou pesquisa quando aprovados.
- **Falhas bloqueantes:** nenhuma falha operacional confirmada; inconsistências encontradas devem ser tratadas como erro de integridade (`VAL-B-07`).
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** relação de atividades pendentes de pesagem, sem concluir se o serviço físico ainda ocorre.

## UC-07 — Consultar e pesquisar histórico

- **Objetivo:** localizar atividades anteriores e seus dados preservados.
- **Informação mínima:** nenhum filtro obrigatório; critérios de busca ainda serão definidos.
- **Precondições:** existência de registros.
- **Fluxo principal:** informar critérios quando desejado e consultar resultados com estado, pesos brutos, instantes e gás obtido da associação imutável do cilindro.
- **Fluxo alternativo:** abrir um registro específico para consulta.
- **Falhas bloqueantes:** nenhuma confirmada; critérios inválidos só poderão ser classificados depois que as regras de consulta forem definidas.
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** histórico consultável sem alterar registros.

## UC-08 — Corrigir registro

- **Objetivo:** ajustar informação incorreta sem apagar silenciosamente o conteúdo anterior.
- **Informação mínima:** registro, alteração solicitada e motivo quando a política exigir.
- **Precondições:** registro existente, operação de correção permitida e política correspondente aprovada.
- **Fluxo principal:** comparar valores atuais e propostos, informar motivo quando exigido, validar, confirmar a excepcionalidade, produzir `correctedAt` e preservar a rastreabilidade.
- **Fluxo alternativo:** desistir sem modificar o registro.
- **Falhas bloqueantes:** nova versão inválida, transição proibida (`VAL-B-06`) ou estado incoerente (`VAL-B-07`).
- **Alertas confirmáveis:** correção excepcional (`VAL-W-06`); a confirmação não supera falhas bloqueantes.
- **Resultado:** valor corrigido e informações anteriores preservadas conforme a política a decidir.

## UC-09 — Cancelar registro

- **Objetivo:** invalidar um registro operacional sem exclusão física.
- **Informação mínima:** registro e motivo, caso o cancelamento seja aprovado.
- **Precondições:** registro existente e política de cancelamento definida.
- **Fluxo principal:** solicitar cancelamento, informar motivo, validar, produzir `cancelledAt` e preservar o registro fora dos cálculos normais.
- **Fluxo alternativo:** desistir e manter o estado anterior.
- **Falhas bloqueantes:** transição inválida (`VAL-B-06`); demais condições, incluindo a exigência de motivo, dependem da política em [OQ-COR-02](open-questions.md#oq-cor-02).
- **Alertas confirmáveis:** nenhum definido enquanto a política de cancelamento permanecer aberta em [OQ-COR-02](open-questions.md#oq-cor-02).
- **Resultado:** registro invalidado de forma auditável, pelo mecanismo ainda a aprovar.

## UC-10 — Consultar informações técnicas do gás

- **Objetivo:** consultar o catálogo sem alterar `operationalName` nem sobrecarregar o registro de atividade.
- **Informação mínima:** gás a consultar.
- **Precondições:** gás existente e informações provenientes de fonte confiável.
- **Fluxo principal:** selecionar o gás e consultar seus dados técnicos disponíveis.
- **Fluxo alternativo:** mostrar somente a identificação quando o catálogo ampliado ainda não existir.
- **Falhas bloqueantes:** nenhuma classificação adicional confirmada; os campos técnicos e as fontes confiáveis do catálogo ampliado dependem de [OQ-CAT-01](open-questions.md#oq-cat-01).
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** informação apresentada fora do formulário operacional e sem alterar atividades.

## UC-11 — Exportar dados

- **Objetivo:** produzir uma cópia utilizável dos dados segundo finalidade confirmada.
- **Informação mínima:** escopo e formato de exportação, ainda em aberto em [OQ-ADM-02](open-questions.md#oq-adm-02).
- **Precondições:** formato, conteúdo e tratamento de registros cancelados aprovados conforme os requisitos pendentes.
- **Fluxo principal:** escolher opções permitidas, gerar e disponibilizar a exportação.
- **Fluxo alternativo:** limitar o período ou os dados conforme requisitos futuros.
- **Falhas bloqueantes:** nenhuma classificação confirmada; critérios de falha dependem dos requisitos ainda abertos em [OQ-ADM-02](open-questions.md#oq-adm-02).
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** arquivo de exportação verificável, sem alterar os dados de origem.

## UC-12 — Realizar backup

- **Objetivo:** criar uma cópia recuperável dos dados internos.
- **Informação mínima:** destino, formato e escopo conforme estratégia ainda aberta em [OQ-ADM-01](open-questions.md#oq-adm-01).
- **Precondições:** política de backup e restauração aprovada.
- **Fluxo principal:** iniciar o backup, verificar sua integridade e informar o resultado.
- **Fluxo alternativo:** cancelar antes da gravação quando seguro.
- **Falhas bloqueantes:** nenhuma classificação confirmada; critérios de falha dependem da política ainda aberta em [OQ-ADM-01](open-questions.md#oq-adm-01).
- **Alertas confirmáveis:** nenhum confirmado; decisões irreversíveis não devem ser automáticas.
- **Resultado:** backup verificável e apto a uma restauração futura definida.

## UC-13 — Marcar cilindro como vazio

- **Objetivo:** indicar manualmente que um cilindro não possui mais refrigerante utilizável.
- **Informação mínima:** cilindro em `ACTIVE` e `finalGrossWeight` medido.
- **Precondições:** cilindro existente; a interação com atividade sem retorno precisa estar resolvida em [OQ-CYL-06](open-questions.md#oq-cyl-06).
- **Fluxo principal:** selecionar o cilindro, acionar “Marcar cilindro como vazio”, informar o peso bruto final, validar, registrar `markedEmptyAt` automaticamente e mudar para `EMPTY`.
- **Fluxo alternativo:** desistir antes da confirmação e manter `ACTIVE`.
- **Falhas bloqueantes:** peso negativo ou informação essencial ausente (`VAL-B-01` e `VAL-B-03`), transição inválida (`VAL-B-06`); conflito com atividade sem retorno permanece pendente em `VAL-P-04`.
- **Alertas confirmáveis:** nenhum confirmado; não existe limite automático de peso vazio.
- **Resultado:** histórico e peso final preservados, cilindro impedido de iniciar atividades e removido das seleções ativas.
