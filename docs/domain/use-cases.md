# Casos de uso

Os casos de uso descrevem objetivos operacionais sem definir telas, rotas, tabelas ou frameworks. Falhas e alertas já classificados usam os IDs da [Matriz de validação](validation-matrix.md), fonte canônica dessas condições. Situações futuras ainda não aprovadas são indicadas como requisitos pendentes ou remetem às [Perguntas abertas](open-questions.md), sem serem tratadas como validações confirmadas.

## UC-01 — Cadastrar gás refrigerante

- **Objetivo:** disponibilizar um tipo de refrigerante para associação com cilindros.
- **Informação mínima:** identificação e nome padronizado, ainda sujeitos a confirmação.
- **Precondições:** critérios mínimos do cadastro aprovados e gás ainda não cadastrado segundo a regra de identidade futura.
- **Fluxo principal:** informar os dados mínimos, validar e registrar o gás.
- **Fluxo alternativo:** corrigir dados antes de confirmar o cadastro.
- **Falhas bloqueantes:** nenhuma classificação adicional confirmada; ausência e duplicidade dependem dos critérios pendentes em [OQ-CAT-01](open-questions.md#oq-cat-01).
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** gás disponível para cadastro de cilindros, sem exigir catálogo técnico completo.

## UC-02 — Cadastrar cilindro

- **Objetivo:** identificar um recipiente físico para uso operacional.
- **Informação mínima:** identificação do cilindro e gás associado; unicidade, formato e demais dados ainda estão abertos.
- **Precondições:** gás existente e política de associação histórica definida.
- **Fluxo principal:** informar identidade, selecionar gás válido e registrar o cilindro.
- **Fluxo alternativo:** registrar o peso inicial somente se sua origem e tratamento forem aprovados.
- **Falhas bloqueantes:** gás inexistente (`VAL-B-08`); identificação ausente ou duplicada permanece requisito pendente em [OQ-CYL-01](open-questions.md#oq-cyl-01).
- **Alertas confirmáveis:** reclassificação excepcional (`VAL-W-07`) não faz parte do cadastro normal.
- **Resultado:** cilindro identificável e apto a participar de atividades segundo as regras aprovadas.

## UC-03 — Registrar atividade concluída

- **Objetivo:** registrar saída e retorno já conhecidos em uma única atividade.
- **Informação mínima:** cilindro, pesos de saída e retorno, local e data operacional.
- **Precondições:** cilindro existente e sem atividade aberta incompatível segundo a política ainda a aprovar.
- **Fluxo principal:** selecionar o cilindro, informar os pesos e os dados operacionais, calcular o consumo e registrar a atividade como concluída.
- **Fluxo alternativo:** confirmar alertas legítimos e continuar sem alterar os dados informados.
- **Falhas bloqueantes:** `VAL-B-01` a `VAL-B-05` e inconsistência de estado `VAL-B-07`.
- **Alertas confirmáveis:** `VAL-W-01` a `VAL-W-05`, quando suas condições e limiares estiverem aprovados.
- **Resultado:** uma `UsageActivity` em `COMPLETED`, com consumo derivado e histórico preservável.

## UC-04 — Registrar saída aguardando retorno

- **Objetivo:** registrar a partida do cilindro antes de o retorno ser conhecido.
- **Informação mínima:** cilindro, peso de saída, local e data de saída.
- **Precondições:** cilindro existente e sem atividade aberta incompatível segundo a política ainda a aprovar.
- **Fluxo principal:** selecionar o cilindro; usar uma sugestão de peso de saída somente quando existir último peso conhecido válido e sua origem tiver sido aprovada em [OQ-WGT-05](open-questions.md#oq-wgt-05) e [OQ-WGT-06](open-questions.md#oq-wgt-06); caso contrário, informar o peso de saída diretamente; depois informar local e data e registrar a saída.
- **Fluxo alternativo:** confirmar uma diferença legítima do último peso conhecido.
- **Falhas bloqueantes:** peso negativo (`VAL-B-01`), ausência obrigatória (`VAL-B-03`), cilindro inexistente (`VAL-B-04`), conflito aberto (`VAL-B-05`) ou estado incoerente (`VAL-B-07`).
- **Alertas confirmáveis:** diferença do último peso (`VAL-W-02`), data passada (`VAL-W-04`) e possível duplicidade (`VAL-W-05`).
- **Resultado:** uma `UsageActivity` em `AWAITING_RETURN`, sem retorno e sem consumo disponível.

## UC-05 — Concluir atividade aberta

- **Objetivo:** registrar o retorno e concluir a mesma atividade iniciada na saída.
- **Informação mínima:** atividade aberta e peso de retorno; data de retorno depende de decisão aberta.
- **Precondições:** atividade em `AWAITING_RETURN` e cilindro correspondente.
- **Fluxo principal:** localizar a atividade, informar o retorno, validar, calcular o consumo e concluir.
- **Fluxo alternativo:** confirmar consumo zero ou alto quando a situação for legítima.
- **Falhas bloqueantes:** peso negativo (`VAL-B-01`), retorno maior que saída (`VAL-B-02`), transição inválida (`VAL-B-06`) ou estado incoerente (`VAL-B-07`).
- **Alertas confirmáveis:** consumo zero (`VAL-W-01`) e consumo alto (`VAL-W-03`).
- **Resultado:** a atividade existente passa a `COMPLETED`; uma segunda atividade não é criada.

## UC-06 — Listar atividades aguardando retorno

- **Objetivo:** identificar cilindros que saíram e ainda não tiveram retorno registrado.
- **Informação mínima:** nenhuma entrada obrigatória; filtros futuros permanecem opcionais.
- **Precondições:** existência de atividades registradas.
- **Fluxo principal:** consultar atividades coerentes com `AWAITING_RETURN`.
- **Fluxo alternativo:** aplicar critérios de ordenação ou pesquisa quando aprovados.
- **Falhas bloqueantes:** nenhuma falha operacional confirmada; inconsistências encontradas devem ser tratadas como erro de integridade (`VAL-B-07`).
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** relação de atividades abertas, sem inventar um estado de disponibilidade duplicado no cilindro.

## UC-07 — Consultar e pesquisar histórico

- **Objetivo:** localizar atividades anteriores e seus dados preservados.
- **Informação mínima:** nenhum filtro obrigatório; critérios de busca ainda serão definidos.
- **Precondições:** existência de registros.
- **Fluxo principal:** informar critérios quando desejado e consultar resultados com estado, dados operacionais e histórico aplicável.
- **Fluxo alternativo:** abrir um registro específico para consulta.
- **Falhas bloqueantes:** nenhuma confirmada; critérios inválidos só poderão ser classificados depois que as regras de consulta forem definidas.
- **Alertas confirmáveis:** nenhum confirmado.
- **Resultado:** histórico consultável sem alterar registros.

## UC-08 — Corrigir registro

- **Objetivo:** ajustar informação incorreta sem apagar silenciosamente o conteúdo anterior.
- **Informação mínima:** registro, alteração solicitada e motivo quando a política exigir.
- **Precondições:** registro existente, operação de correção permitida e política correspondente aprovada.
- **Fluxo principal:** comparar valores atuais e propostos, informar motivo, validar, confirmar a excepcionalidade e preservar a rastreabilidade.
- **Fluxo alternativo:** desistir sem modificar o registro.
- **Falhas bloqueantes:** nova versão inválida, transição proibida (`VAL-B-06`) ou estado incoerente (`VAL-B-07`).
- **Alertas confirmáveis:** correção excepcional (`VAL-W-06`); a confirmação não supera falhas bloqueantes.
- **Resultado:** valor corrigido e informações anteriores preservadas conforme a política a decidir.

## UC-09 — Cancelar registro

- **Objetivo:** invalidar um registro operacional sem exclusão física.
- **Informação mínima:** registro e motivo, caso o cancelamento seja aprovado.
- **Precondições:** registro existente e política de cancelamento definida.
- **Fluxo principal:** solicitar cancelamento, informar motivo, validar e preservar o registro fora dos cálculos normais.
- **Fluxo alternativo:** desistir e manter o estado anterior.
- **Falhas bloqueantes:** transição inválida (`VAL-B-06`); demais condições, incluindo a exigência de motivo, dependem da política em [OQ-COR-02](open-questions.md#oq-cor-02).
- **Alertas confirmáveis:** nenhum definido enquanto a política de cancelamento permanecer aberta em [OQ-COR-02](open-questions.md#oq-cor-02).
- **Resultado:** registro invalidado de forma auditável, pelo mecanismo ainda a aprovar.

## UC-10 — Consultar informações técnicas do gás

- **Objetivo:** consultar o catálogo sem sobrecarregar o registro de atividade.
- **Informação mínima:** gás a consultar.
- **Precondições:** gás existente e informações provenientes de fonte confiável.
- **Fluxo principal:** selecionar o gás e consultar seus dados técnicos disponíveis.
- **Fluxo alternativo:** mostrar somente a identificação quando o catálogo ampliado ainda não existir.
- **Falhas bloqueantes:** nenhuma classificação adicional confirmada; existência, campos mínimos e fontes confiáveis dependem de [OQ-CAT-01](open-questions.md#oq-cat-01).
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
