# Perguntas abertas e registro de decisões

Este documento registra decisões que exigem evidência humana. Os estados permitidos são `Aberta`, `Respondida` e `Adiada`. Uma pergunta marcada como bloqueadora impede a parte afetada do Milestone 2B até receber decisão final.

A planilha original não estava disponível nesta sessão e não foi inspecionada. Ela deve ser revisada antes de implementar regras relacionadas a unidade, precisão, identificação, fórmulas, duplicidade, correção ou representação de atividades abertas.

## Pesos

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-wgt-01"></a>OQ-WGT-01 | Peso | Qual unidade é usada? | Aberta | Não assumir unidade. | planilha, balança e procedimento operacional | responsável operacional | representação e mensagens | Sim, para `Weight` | — |
| <a id="oq-wgt-02"></a>OQ-WGT-02 | Peso | Quantas casas decimais devem ser usadas, qual é a escala ou precisão operacional e qual regra de arredondamento deve ser aplicada? | Aberta | Preservar a precisão real, evitar `double` e não escolher arredondamento sem evidência. | especificação da balança, amostras da planilha e procedimento operacional de arredondamento | responsável operacional | `BigDecimal`, comparação, arredondamento e consumo zero | Sim, para `Weight` | — |
| <a id="oq-wgt-03"></a>OQ-WGT-03 | Peso | O registro usa peso bruto ou líquido? | Aberta | Usar o conceito empregado pela operação, sem conversão inventada. | procedimento e exemplos reais | responsável operacional | significado de todos os pesos | Sim | — |
| <a id="oq-wgt-04"></a>OQ-WGT-04 | Peso | Como a tara é tratada? | Aberta | Não adicionar tara ao modelo sem necessidade comprovada. | procedimento, cilindros e balança | responsável operacional | cálculo e cadastro do cilindro | Sim se o peso for líquido | — |
| <a id="oq-wgt-05"></a>OQ-WGT-05 | Peso | Existe peso inicial no cadastro do cilindro e qual é sua origem? | Aberta | Registrar somente se for medido e operacionalmente confiável. | cadastro atual e exemplos | responsável operacional | primeira sugestão de saída | Sim para sugestão inicial | — |
| <a id="oq-wgt-06"></a>OQ-WGT-06 | Peso | Recarga, transferência, manutenção, recalibração, correção manual, substituição ou desativação existem na operação e como alteram o peso conhecido? | Aberta | Não criar consumo fictício; avaliar registro próprio apenas com evidência. | procedimentos e casos reais | operação e manutenção | origem de `lastKnownWeight` | Sim para sugestão de peso | — |
| <a id="oq-wgt-07"></a>OQ-WGT-07 | Peso | Qual diferença do último peso conhecido é relevante? | Aberta | Alerta confirmável, nunca bloqueio, após definir tolerância. | precisão da balança e exemplos | responsável operacional | `VAL-W-02` | Sim para esse alerta | — |
| <a id="oq-wgt-08"></a>OQ-WGT-08 | Peso | O que caracteriza consumo excepcionalmente alto? | Aberta | Usar limiar validado, possivelmente contextual, sem inventar limite. | histórico e avaliação operacional | responsável operacional | `VAL-W-03` | Sim para esse alerta | — |

## Cilindros e refrigerantes

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-cyl-01"></a>OQ-CYL-01 | Cilindro | Cada cilindro possui identificação única confiável? | Aberta | Exigir identidade operacional antes de registrar atividades por cilindro. | etiquetas, cadastro e planilha | responsável operacional | identidade de `Cylinder` | Sim | — |
| <a id="oq-cyl-02"></a>OQ-CYL-02 | Cilindro | A planilha registra cilindros ou somente tipos de gás? | Aberta | Priorizar cilindro se a operação conseguir identificá-lo. | inspeção da planilha e demonstração do fluxo | responsável operacional | seleção e migração futura | Sim | — |
| <a id="oq-cyl-03"></a>OQ-CYL-03 | Relação | Um cilindro pode mudar de refrigerante? Em quais condições? | Aberta | Não permitir mudança livre sem procedimento e rastreabilidade. | procedimentos de limpeza, recarga e reuso | operação e manutenção | invariantes e reclassificação | Sim | — |
| <a id="oq-cyl-04"></a>OQ-CYL-04 | Histórico | Como preservar o gás histórico: associação imutável, cópia na atividade ou relação com vigência? | Aberta | Escolher somente após responder se há reclassificação e qual auditoria é necessária. | casos reais, política operacional e necessidade de consulta histórica | responsável operacional e revisão técnica | relação entre as três entidades candidatas | Sim | — |
| <a id="oq-cyl-05"></a>OQ-CYL-05 | Ciclo do cilindro | Como substituição ou desativação afeta atividades abertas e histórico? | Aberta | Preservar atividades e impedir novas operações quando apropriado. | casos reais e procedimento patrimonial | operação e manutenção | ciclo do cilindro | Sim se fizer parte do 2B | — |

## Atividades

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-act-01"></a>OQ-ACT-01 | Atividade | Um cilindro pode ter mais de uma atividade aberta? O que significa “incompatível”? | Aberta | Permitir no máximo uma atividade aberta por cilindro até evidência contrária. | situações reais de uso simultâneo | responsável operacional | disponibilidade e `VAL-B-05` | Sim | — |
| <a id="oq-act-02"></a>OQ-ACT-02 | Atividade | Quais campos mínimos identificam uma atividade no histórico? | Aberta | Usar identidade própria interna sem expor campo adicional no formulário. | consultas atuais e planilha | operação e revisão técnica | identidade de `UsageActivity` | Sim para implementação completa | — |
| <a id="oq-act-03"></a>OQ-ACT-03 | Local | Local é texto livre ou existe padrão operacional estável? | Aberta | Manter atributo simples; sugestões não criam entidade. | valores reais da planilha | responsável operacional | validação e pesquisa | Não para núcleo inicial | — |

<a id="datas-e-tempo"></a>
## Datas e tempo

| ID | Área | Pergunta | Status | Recomendação atual | Evidência necessária | Responsável esperado | Impacto | Bloqueia o Milestone 2B? | Decisão final |
|---|---|---|---|---|---|---|---|---|---|
| <a id="oq-dat-01"></a>OQ-DAT-01 | Datas | Saída e retorno podem ocorrer em datas diferentes? | Aberta | Representar a diferença se ela for operacionalmente relevante. | exemplos de atividades abertas | responsável operacional | campos e ciclo de vida | Sim | — |
| <a id="oq-dat-02"></a>OQ-DAT-02 | Datas | A pessoa precisa informar a data de retorno? | Aberta | Não adicionar campo visível se o instante automático atender à operação. | necessidade de relatórios e casos reais | responsável operacional | conclusão da atividade | Sim | — |
| <a id="oq-dat-03"></a>OQ-DAT-03 | Auditoria | Um instante automático de conclusão é suficiente? | Aberta | Registrar instante técnico automaticamente e separar de data operacional. | requisitos de consulta e rastreabilidade | operação e revisão técnica | auditoria | Sim para instantes de auditoria do Milestone 2B, se incluídos | — |
| <a id="oq-dat-04"></a>OQ-DAT-04 | Datas | Qual fuso horário define “hoje”? | Aberta | Definir explicitamente o fuso operacional; não depender da máquina. | local de operação e política futura | responsável do produto | valor padrão e validações | Sim para regras relativas a hoje | — |
| <a id="oq-dat-05"></a>OQ-DAT-05 | Datas | Datas operacionais futuras são permitidas? | Aberta | Não decidir classificação sem caso operacional. | planilha e procedimento | responsável operacional | `VAL-P-01` | Sim para essa validação | — |
| <a id="oq-dat-06"></a>OQ-DAT-06 | Datas | Quando uma data passada exige confirmação? | Aberta | Evitar alertar datas passadas legítimas; definir condição objetiva. | frequência de lançamentos retroativos | responsável operacional | `VAL-W-04` | Sim para esse alerta | — |

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
| <a id="oq-opt-01"></a>OQ-OPT-01 | Campos opcionais | Com que frequência ordem de serviço, técnico, observações, horários e detalhes são realmente usados? | Aberta | Manter fora do MVP e não alertar ausência até comprovação. | amostra da planilha e entrevistas | responsável operacional | escopo e simplicidade | Não para núcleo | — |
| <a id="oq-adm-01"></a>OQ-ADM-01 | Backup | Qual formato, destino, frequência e verificação são necessários para backup? | Aberta | Não escolher infraestrutura antes do requisito. | rotina atual e necessidade de restauração | responsável do produto | caso de uso de backup | Não para domínio puro; sim para futuro backup | — |
| <a id="oq-adm-02"></a>OQ-ADM-02 | Exportação | Qual formato, conteúdo e finalidade são necessários para exportação? | Aberta | Definir pelo consumidor real dos dados. | exemplos de uso e arquivos esperados | responsável do produto | caso de uso de exportação | Não para domínio puro; sim para futura exportação | — |
| <a id="oq-aud-01"></a>OQ-AUD-01 | Autoria | Como registrar autoria enquanto não existe autenticação? | Aberta | Documentar a limitação e não criar segurança neste marco. | necessidade real de atribuição | responsável do produto | rastreabilidade | Não para núcleo do 2B | — |
| <a id="oq-cat-01"></a>OQ-CAT-01 | Catálogo | Quais campos mínimos e fontes técnicas são confiáveis, e quem os mantém? | Aberta | MVP com identificação mínima; catálogo ampliado somente com fonte responsável. | normas, fabricantes e responsável técnico | responsável técnico do catálogo | `RefrigerantGas` e confiabilidade | Sim para catálogo ampliado; não para conceito mínimo | — |

## Evidências a obter na planilha

Antes das partes afetadas do Milestone 2B, revisar:

- nomes, ordem e significado das colunas;
- formato, precisão e exemplos de peso;
- fórmulas existentes, sem presumir que estejam corretas;
- presença de identificadores de cilindro;
- forma de representar saídas ainda sem retorno;
- datas vazias, retroativas ou futuras;
- padrões reais de gases e locais;
- duplicidades, sobrescritas, correções e exclusões;
- frequência dos campos opcionais;
- volume e período do histórico;
- usos atuais de filtro, backup e exportação.

Uma resposta só muda para `Respondida` quando a decisão final e sua evidência forem registradas. `Adiada` exige justificativa e não autoriza implementar a parte bloqueada.
