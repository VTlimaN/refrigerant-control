# Perguntas e registro de decisões

Este documento registra decisões respondidas e perguntas que ainda exigem evidência humana. Os estados usados neste marco são `Respondida` e `Aberta`. Uma pergunta bloqueadora impede somente a parte afetada do Milestone 2B.

A planilha original não estava disponível nesta sessão e não foi inspecionada pelo Codex. As respostas deste marco usam confirmação do responsável operacional e resumo de análise externa da planilha. O arquivo ainda será necessário para migração e para assuntos que permanecem abertos.

Existem 37 IDs: 19 com status `Respondida` e 18 com status `Aberta`.

## Pesos

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-wgt-01"></a>OQ-WGT-01 | Peso | Qual unidade é usada? | Respondida | Usar quilograma (`kg`). | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | representação e mensagens | Não | Quilograma é a unidade operacional. |
| <a id="oq-wgt-02"></a>OQ-WGT-02 | Peso | Qual é a resolução observada e como tratar representações com zeros finais? | Respondida | Usar `BigDecimal`, resolução de `0,01 kg` e equivalência numérica entre `15,14` e `15,140`; apresentação visual não é regra do domínio. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | comparação e consumo zero | Não para os aspectos respondidos; arredondamento está em OQ-WGT-09 | A escala textual não representa precisão física adicional. |
| <a id="oq-wgt-03"></a>OQ-WGT-03 | Peso | O registro usa peso bruto ou líquido? | Respondida | Usar peso bruto do cilindro com o refrigerante restante. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | significado dos pesos | Não | Todo peso operacional é bruto. |
| <a id="oq-wgt-04"></a>OQ-WGT-04 | Peso | Como a tara é tratada? | Respondida | Não subtrair tara; cada cilindro vazio tem peso diferente e não há valor universal. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | cálculo e cadastro | Não | Conteúdo líquido nominal, peso bruto e tara são distintos. |
| <a id="oq-wgt-05"></a>OQ-WGT-05 | Peso | Quando o peso bruto inicial deve existir e qual é sua origem? | Respondida | Medir no recebimento e garantir o valor antes da primeira atividade; cadastro e medição podem ocorrer juntos ou separados. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | primeira sugestão e elegibilidade para uso | Não | `initialGrossWeight` pertence ao cilindro e não cria atividade. |
| <a id="oq-wgt-06"></a>OQ-WGT-06 | Peso | Transferência, manutenção, recalibração ou correção manual existem e como alteram o peso conhecido? | Aberta | Não criar consumo fictício nem evento próprio sem evidência. Recarga foi excluída porque o cilindro não é recarregável. | procedimentos e casos reais | operação e manutenção | fontes adicionais de `lastKnownGrossWeight` | Sim somente para eventos adicionais | — |
| <a id="oq-wgt-07"></a>OQ-WGT-07 | Peso | Qual diferença do último peso bruto conhecido é relevante? | Aberta | Alerta confirmável, nunca bloqueio, após definir tolerância. | exemplos reais e resolução da balança | responsável operacional | `VAL-W-02` | Sim para esse alerta | — |
| <a id="oq-wgt-08"></a>OQ-WGT-08 | Peso | O que caracteriza consumo excepcionalmente alto? | Aberta | Não inventar limiar. | histórico e avaliação operacional | responsável operacional | `VAL-W-03` | Sim para esse alerta | — |
| <a id="oq-wgt-09"></a>OQ-WGT-09 | Peso | Valores incompatíveis com incremento de `0,01 kg` devem ser rejeitados ou arredondados e, se arredondados, com qual modo? | Aberta | Preferir rejeição até decisão explícita; nunca arredondar silenciosamente. | procedimento de entrada e decisão operacional | operação e revisão técnica | `VAL-P-03` e construção de `Weight` | Sim | — |
| <a id="oq-wgt-10"></a>OQ-WGT-10 | Peso | Qual é a capacidade máxima da balança? | Aberta | Não criar limite máximo sem especificação do equipamento. | identificação ou manual da balança | responsável operacional | eventual validação máxima | Não para o núcleo; sim para qualquer limite máximo | — |

## Cilindros e refrigerantes

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-cyl-01"></a>OQ-CYL-01 | Cilindro | Cada cilindro possui identificação única confiável? | Respondida | Usar `sealNumber`, único, permanente e imutável. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | identidade de `Cylinder` | Não | O número do lacre identifica um cilindro. |
| <a id="oq-cyl-02"></a>OQ-CYL-02 | Cilindro | A planilha distingue cilindros ou somente tipos de gás? | Respondida | Os lacres distinguem cilindros; linhas sem retorno ainda podem ser ambíguas para importação. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | seleção e migração | Não para identidade; importação exige revisão humana | O cilindro é a seleção operacional principal. |
| <a id="oq-cyl-03"></a>OQ-CYL-03 | Relação | Um cilindro pode mudar de refrigerante? | Respondida | Não. O cilindro não é recarregável e a associação é imutável. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | operação e manutenção | invariantes | Não | Alteração de refrigerante é proibida. |
| <a id="oq-cyl-04"></a>OQ-CYL-04 | Histórico | Como preservar o gás histórico? | Respondida | Derivar o gás da associação imutável do cilindro. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional e revisão técnica | relação entre entidades | Não | Snapshot e vigência temporal não são necessários. |
| <a id="oq-cyl-05"></a>OQ-CYL-05 | Ciclo do cilindro | Como representar um cilindro que ficou sem refrigerante utilizável? | Respondida | Usar ciclo candidato `ACTIVE`/`EMPTY`; marcar manualmente, preservar peso final e histórico e impedir novos usos. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | operação e manutenção | `CylinderStatus` | Não para o ciclo mínimo; conflito com atividade pendente está em OQ-CYL-06 | Não criar etapa de descarte físico no MVP. |
| <a id="oq-cyl-06"></a>OQ-CYL-06 | Ciclo do cilindro | Como marcar o cilindro vazio quando ainda existe atividade sem peso de retorno? | Aberta | Não concluir atividade nem reutilizar o peso automaticamente. | casos reais e procedimento operacional | responsável operacional | UC-13 e `VAL-P-04` | Sim para essa transição | — |

## Atividades

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-act-01"></a>OQ-ACT-01 | Atividade | Um cilindro pode ter mais de uma atividade sem peso de retorno? | Respondida | Não permitir nova atividade até registrar o retorno da anterior. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | disponibilidade e `VAL-B-05` | Não | Sem pesagem intermediária, o consumo não pode ser atribuído corretamente. |
| <a id="oq-act-02"></a>OQ-ACT-02 | Atividade | Quais campos mínimos identificam uma atividade no histórico? | Aberta | Usar identidade própria interna sem expor campo adicional no formulário. | consultas atuais e planilha | operação e revisão técnica | identidade de `UsageActivity` | Sim para implementação completa | — |
| <a id="oq-act-03"></a>OQ-ACT-03 | Local | O local da atividade é obrigatório e qual formato utiliza? | Respondida | Usar texto livre obrigatório, sem normalização nem entidade própria. | decisão operacional para o Milestone 2C.3A | responsável operacional | início, consulta e histórico | Não | Rejeitar valor nulo, vazio ou somente espaços e preservar exatamente todo valor não vazio. |
| <a id="oq-act-04"></a>OQ-ACT-04 | Atividade | Criar diretamente em `COMPLETED` pertence ao fluxo normal? | Respondida | Não. Restringir a importação, correção histórica ou recuperação controlada futura. | confirmação operacional na revisão e contexto do resumo da análise externa; planilha indisponível nesta sessão | responsável do produto | UC-03 e política temporal | Não para o fluxo normal; exceção depende de OQ-DAT-05, OQ-DAT-06 e OQ-COR-01 | Não inventar instantes históricos. |

<a id="datas-e-tempo"></a>
## Datas e tempo

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-dat-01"></a>OQ-DAT-01 | Tempo | Saída e pesagem de retorno podem ocorrer em dias diferentes? | Respondida | Sim; representar os fatos por `startedAt` e `completedAt`. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | ciclo de vida | Não | A ausência de retorno pode atravessar dias. |
| <a id="oq-dat-02"></a>OQ-DAT-02 | Tempo | A pessoa precisa informar data ou hora de retorno no fluxo normal? | Respondida | Não; registrar `completedAt` automaticamente ao informar o peso. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | conclusão | Não | Nenhum campo temporal manual normal. |
| <a id="oq-dat-03"></a>OQ-DAT-03 | Auditoria | Instantes automáticos atendem ao fluxo normal? | Respondida | Sim; produzir `startedAt`, `completedAt`, `correctedAt`, `cancelledAt` e `markedEmptyAt` nos eventos correspondentes. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | operação e revisão técnica | rastreabilidade | Não para o fluxo normal | Exceções históricas permanecem abertas. |
| <a id="oq-dat-04"></a>OQ-DAT-04 | Tempo | Qual fuso define exibição e data civil? | Respondida | Usar `America/Sao_Paulo`. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável do produto | exibição e derivação local | Não | Não depender do fuso da máquina. |
| <a id="oq-dat-05"></a>OQ-DAT-05 | Exceções temporais | Como tratar valores futuros em importação ou correção histórica? | Aberta | Não se aplica ao fluxo normal; não decidir sem casos reais. | dados legados e política de correção | responsável operacional | `VAL-P-01` | Sim somente para operações excepcionais | — |
| <a id="oq-dat-06"></a>OQ-DAT-06 | Exceções temporais | Quando valor passado informado em importação ou correção exige confirmação? | Aberta | Não alertar automaticamente sem condição objetiva. | dados legados e casos de correção | responsável operacional | `VAL-P-01` | Sim somente para operações excepcionais | — |

## Validação, correção e cancelamento

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-val-01"></a>OQ-VAL-01 | Duplicidade | Quais campos e qual janela caracterizam possível registro duplicado? | Aberta | Usar alerta confirmável, nunca eliminação automática. | duplicidades reais da planilha | responsável operacional | `VAL-W-05` | Sim para esse alerta | — |
| <a id="oq-cor-01"></a>OQ-COR-01 | Correção | Quais registros e campos podem ser corrigidos, quando o motivo é obrigatório e pode haver reabertura? | Aberta | Correção explícita, motivo para mudança material e sem reabertura automática. | procedimento atual e casos de erro | responsável operacional | ciclo e rastreabilidade | Sim | — |
| <a id="oq-cor-02"></a>OQ-COR-02 | Cancelamento | Cancelamento é `ActivityStatus.CANCELLED` ou operação auditável separada? | Aberta | Preservar estado anterior e excluir o registro dos cálculos normais em qualquer opção. | necessidades de consulta, exportação e auditoria | operação e revisão técnica | enum candidato e transições | Sim | — |
| <a id="oq-cor-03"></a>OQ-COR-03 | Exclusão | Exclusão física deve ser proibida, restrita ou exigida em alguma situação? | Aberta | Proibir na operação normal e preferir cancelamento. | obrigações operacionais ou legais | responsável do produto | integridade histórica | Sim para política de exclusão | — |
| <a id="oq-cor-04"></a>OQ-COR-04 | Rastreabilidade | Quais valores, motivos e confirmações precisam ser preservados? | Aberta | Preservar antes/depois, motivo, tipo de evento e instante. | requisitos de auditoria e exemplos | responsável do produto | modelo de correção futuro | Sim | — |

## Informações opcionais e funções administrativas

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-opt-01"></a>OQ-OPT-01 | Campos opcionais | Quais informações ficam fora do fluxo obrigatório? | Respondida | Ordem de serviço, técnico e observações são opcionais; horários normais são automáticos; ausência não alerta. O local foi confirmado como obrigatório em OQ-ACT-03. | confirmação operacional, resumo da análise externa e decisão do Milestone 2C.3A; planilha indisponível nesta sessão | responsável operacional | escopo e simplicidade | Não | Usar divulgação progressiva somente para ordem de serviço, técnico e observações. |
| <a id="oq-adm-01"></a>OQ-ADM-01 | Backup | Qual formato, destino, frequência e verificação são necessários para backup? | Aberta | Não escolher infraestrutura antes do requisito. | rotina atual e necessidade de restauração | responsável do produto | caso de uso de backup | Não para domínio puro; sim para futuro backup | — |
| <a id="oq-adm-02"></a>OQ-ADM-02 | Exportação | Qual formato, conteúdo e finalidade são necessários para exportação? | Aberta | Definir pelo consumidor real dos dados. | exemplos de uso e arquivos esperados | responsável do produto | caso de uso de exportação | Não para domínio puro; sim para futura exportação | — |
| <a id="oq-aud-01"></a>OQ-AUD-01 | Autoria | Como registrar autoria enquanto não existe autenticação? | Aberta | Documentar a limitação e não criar segurança neste marco. | necessidade real de atribuição | responsável do produto | rastreabilidade | Não para núcleo do 2B | — |
| <a id="oq-cat-01"></a>OQ-CAT-01 | Catálogo | Quais campos ampliados, fontes técnicas e responsáveis são confiáveis? | Aberta | Catálogo ampliado somente com fonte responsável. | normas, fabricantes e responsável técnico | responsável técnico do catálogo | confiabilidade técnica | Sim para catálogo ampliado; não para conceito mínimo | — |
| <a id="oq-cat-02"></a>OQ-CAT-02 | Catálogo operacional | Como preservar os nomes usados nos cilindros? | Respondida | Manter exatamente `R410A`, `R32`, `R-22`, `R407C`, `R134A`, `R404` e `141B`, sem renomeação automática. | confirmação operacional e resumo da análise externa; planilha indisponível nesta sessão | responsável operacional | `operationalName` | Não | Descrições técnicas futuras não substituem o rótulo. |

## Evidências ainda necessárias

Antes das partes afetadas do Milestone 2B ou de funcionalidades posteriores, confirmar:

- valores incompatíveis com `0,01 kg`, capacidade da balança e tolerâncias;
- transferência, manutenção, recalibração e correção manual de peso;
- consumo alto e duplicidade;
- identidade interna da atividade;
- políticas de correção, cancelamento, exclusão e autoria;
- tratamento temporal de importações e correções históricas;
- marcação como vazio com atividade sem retorno;
- formatos de backup e exportação;
- fontes do catálogo técnico.

Na migração, linhas com saída e sem retorno devem ser classificadas manualmente como peso inicial ou atividade. Nenhuma resposta autoriza implementação Java neste marco.
