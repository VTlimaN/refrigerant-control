# Matriz de validação

Esta é a fonte canônica para classificar erros bloqueantes, alertas confirmáveis e situações pendentes. A unidade e a resolução observada estão confirmadas, mas capacidade, arredondamento, tolerâncias e limiares não são definidos sem evidência.

| ID | Situação | Classificação | Condição | Dados necessários | Pode continuar? | Mensagem esperada em português | Informação a registrar | Estado da decisão | Pergunta relacionada |
|---|---|---|---|---|---|---|---|---|---|
| VAL-B-01 | Peso negativo | Erro bloqueante | qualquer peso bruto informado é menor que zero | peso informado e operação | Não | “O peso não pode ser negativo.” | tentativa rejeitada, se necessário | Confirmada | [OQ-WGT-01](open-questions.md#oq-wgt-01) e [OQ-WGT-02](open-questions.md#oq-wgt-02) |
| VAL-B-02 | Retorno maior que saída | Erro bloqueante | `returnGrossWeight` maior que `departureGrossWeight` | ambos os pesos | Não | “O peso de retorno não pode ser maior que o peso de saída.” | valores rejeitados, se necessário | Confirmada | — |
| VAL-B-03 | Informação essencial ausente | Erro bloqueante | falta dado exigido pela operação: lacre ou gás no cadastro; peso inicial antes do primeiro uso; cilindro ou saída no início; o local da atividade é obrigatório, valores nulos, vazios ou compostos apenas por espaços são inválidos e todo valor não branco é preservado exatamente; retorno na conclusão; peso final na marcação como vazio | caso de uso, campos e estado pretendido | Não | “Preencha as informações obrigatórias para continuar.” | campos rejeitados, sem criar estado inválido | Confirmada | [OQ-WGT-05](open-questions.md#oq-wgt-05), [OQ-ACT-03](open-questions.md#oq-act-03) e [OQ-OPT-01](open-questions.md#oq-opt-01) |
| VAL-B-04 | Cilindro inexistente | Erro bloqueante | `sealNumber` não corresponde a cilindro cadastrado | lacre | Não | “O cilindro informado não existe.” | lacre rejeitado, quando necessário | Confirmada | [OQ-CYL-01](open-questions.md#oq-cyl-01) |
| VAL-B-05 | Atividade anterior sem pesagem de retorno | Erro bloqueante | cilindro já possui atividade sem `returnGrossWeight` | cilindro e atividades pendentes | Não | “Este cilindro já possui uma atividade aguardando pesagem de retorno.” | atividade conflitante | Confirmada | [OQ-ACT-01](open-questions.md#oq-act-01) |
| VAL-B-06 | Transição inválida | Erro bloqueante | mudança não permitida pelo ciclo de vida | estado atual, operação e campos | Não | “Esta alteração não é permitida para o estado atual.” | estado e operação rejeitada | Confirmada; políticas excepcionais abertas | [OQ-COR-01](open-questions.md#oq-cor-01) e [OQ-COR-02](open-questions.md#oq-cor-02) |
| VAL-B-07 | Estado e campos inconsistentes | Erro bloqueante | `AWAITING_RETURN_WEIGHT` possui retorno, conclusão ou consumo; `COMPLETED` não possui ambos os pesos ou a evidência temporal exigida por sua origem; registro invalidado participa do consumo normal | estado, pesos, instantes, origem, consumo e invalidação | Não | “O estado da atividade não corresponde às informações registradas.” | combinação rejeitada | Confirmada; forma de cancelamento e política temporal excepcional abertas | [OQ-ACT-04](open-questions.md#oq-act-04), [OQ-DAT-05](open-questions.md#oq-dat-05), [OQ-DAT-06](open-questions.md#oq-dat-06) e [OQ-COR-02](open-questions.md#oq-cor-02) |
| VAL-B-08 | Gás inexistente | Erro bloqueante | cilindro referencia refrigerante não cadastrado | gás operacional | Não | “O gás refrigerante informado não existe.” | referência rejeitada | Confirmada | [OQ-CAT-02](open-questions.md#oq-cat-02) |
| VAL-B-09 | Número do lacre duplicado | Erro bloqueante | `sealNumber` já identifica outro cilindro | lacre e cilindros cadastrados | Não | “Já existe um cilindro com este número de lacre.” | lacre rejeitado | Confirmada | [OQ-CYL-01](open-questions.md#oq-cyl-01) |
| VAL-B-10 | Alteração de dados imutáveis do cilindro | Erro bloqueante | tentativa de mudar `sealNumber` ou refrigerante depois do cadastro | cilindro e valores propostos | Não | “O lacre e o gás do cilindro não podem ser alterados.” | alteração rejeitada | Confirmada | [OQ-CYL-01](open-questions.md#oq-cyl-01) e [OQ-CYL-03](open-questions.md#oq-cyl-03) |
| VAL-B-11 | Nova atividade com cilindro vazio | Erro bloqueante | cilindro em `EMPTY` tenta iniciar atividade | cilindro e estado | Não | “Este cilindro está vazio e não pode iniciar uma nova atividade.” | operação rejeitada | Confirmada | [OQ-CYL-05](open-questions.md#oq-cyl-05) |
| VAL-W-01 | Consumo zero | Alerta confirmável | pesos numericamente equivalentes, inclusive com escalas textuais diferentes | ambos os pesos | Sim | “O consumo calculado é zero. Deseja continuar?” | confirmação, valores e instante | Confirmada como alerta | [OQ-WGT-02](open-questions.md#oq-wgt-02) |
| VAL-W-02 | Saída diferente do último peso bruto conhecido | Alerta confirmável | diferença considerada relevante | saída, último peso, origem cronológica e tolerância | Sim | “O peso de saída está diferente do último peso conhecido. Deseja continuar?” | valores, origem, critério, confirmação e instante | Aberta somente quanto à tolerância e fontes adicionais | [OQ-WGT-05](open-questions.md#oq-wgt-05), [OQ-WGT-06](open-questions.md#oq-wgt-06) e [OQ-WGT-07](open-questions.md#oq-wgt-07) |
| VAL-W-03 | Consumo excepcionalmente alto | Alerta confirmável | consumo supera critério aprovado | pesos, consumo e limiar | Sim | “O consumo informado está acima do esperado. Deseja continuar?” | consumo, critério, confirmação e instante | Aberta | [OQ-WGT-08](open-questions.md#oq-wgt-08) |
| VAL-W-05 | Possível duplicidade | Alerta confirmável | registro atende a critérios ainda não definidos | cilindro, pesos, instantes e histórico necessário | Sim | “Pode existir um registro semelhante. Deseja continuar?” | registros comparados, critério e confirmação | Aberta | [OQ-VAL-01](open-questions.md#oq-val-01) |
| VAL-W-06 | Correção excepcional | Alerta confirmável | alteração permitida em registro salvo | antes, depois, estado e motivo | Sim, se a correção for válida | “Esta correção alterará um registro existente. Deseja continuar?” | valores, motivo, confirmação e `correctedAt` | Aberta | [OQ-COR-01](open-questions.md#oq-cor-01) |
| VAL-P-01 | Instante excepcional passado ou futuro | Classificação pendente | importação ou correção fornece data ou instante diferente do momento do registro | origem, valor temporal e fuso | A decidir | Mensagem ainda não definida | a decidir | Aberta; não se aplica ao fluxo normal automático | [OQ-DAT-05](open-questions.md#oq-dat-05) e [OQ-DAT-06](open-questions.md#oq-dat-06) |
| VAL-P-03 | Peso incompatível com resolução observada | Classificação pendente | valor não é compatível com incremento de `0,01 kg` | texto informado e valor numérico | A decidir | Mensagem depende de rejeição ou arredondamento aprovado | nenhuma transformação silenciosa | Aberta | [OQ-WGT-09](open-questions.md#oq-wgt-09) |
| VAL-P-04 | Marcação como vazio com atividade sem retorno | Classificação pendente | cilindro possui atividade sem `returnGrossWeight` | cilindro, atividade e peso final | A decidir | Mensagem ainda não definida | a decidir | Aberta | [OQ-CYL-06](open-questions.md#oq-cyl-06) |

## Validações substituídas

| ID anterior | Motivo | Tratamento vigente |
|---|---|---|
| `VAL-W-04` | Data passada não é informada no fluxo normal. | `VAL-P-01` cobre somente operações excepcionais. |
| `VAL-W-07` | Refrigerante do cilindro é imutável. | A tentativa é bloqueada por `VAL-B-10`. |
| `VAL-P-02` | Campo opcional ausente não é validação. | Nenhum alerta por ausência de ordem de serviço, técnico ou observações; o local da atividade é obrigatório por `VAL-B-03`. |

## Aplicação da matriz

- Um erro bloqueante impede salvar ou concluir.
- Um alerta confirmável permite continuar somente depois de confirmação consciente.
- Uma confirmação não altera a validade de pesos, referências ou estado.
- “Classificação pendente” não autoriza implementação até a pergunta relacionada ser decidida.
- Escala de `BigDecimal` e quantidade de casas exibidas não substituem a resolução física confirmada.
- Não existe validação de peso máximo ou vazio automático sem capacidade e procedimento aprovados.
- Mensagens podem ser refinadas durante o projeto de interface, sem mudar a classificação da regra.
