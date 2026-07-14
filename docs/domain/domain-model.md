# Modelo de domínio candidato

Este documento descreve conceitos candidatos, não classes Java prontas. O objetivo é tornar responsabilidades e relações compreensíveis antes de escolher detalhes de implementação no Milestone 2B.

As regras obrigatórias estão em [Regras de negócio](business-rules.md), e os estados estão em [Ciclo de vida da atividade](activity-lifecycle.md).

## Entidades candidatas

### `RefrigerantGas`

É candidato a entidade porque um tipo de refrigerante possui identidade e informações próprias, existe antes dos cilindros e pode ser associado a vários deles. O cadastro mínimo pertence ao MVP; o catálogo técnico ampliado é separado da operação.

`operationalName` preserva exatamente o rótulo usado na operação. Os nomes confirmados estão no [Glossário](glossary.md#nomes-operacionais-confirmados). Fontes e campos técnicos ampliados continuam abertos.

### `Cylinder`

É candidato a entidade porque um recipiente físico precisa ser distinguido dos demais e participa de atividades ao longo do tempo. `sealNumber` é sua identidade operacional única, permanente e imutável.

Um cilindro não é recarregável e sua associação com `RefrigerantGas` é imutável. Seu ciclo mínimo candidato é `ACTIVE` ou `EMPTY`. A disponibilidade para nova atividade combina esse estado com a ausência de outra atividade sem peso de retorno; não exige um booleano duplicado.

`initialGrossWeight` deve existir antes da primeira atividade, mas não precisa ser informado na mesma operação que cadastra lacre e refrigerante.

### `UsageActivity`

É candidato a entidade porque representa um fato operacional identificável, muda de estado e precisa preservar sua história em correções e cancelamentos.

No fluxo normal, uma `UsageActivity` começa em `AWAITING_RETURN_WEIGHT` e passa a `COMPLETED` quando a pesagem de retorno é registrada. Não serão criados conceitos separados `OpenActivity` e `CompletedActivity`.

A criação direta em `COMPLETED` fica restrita a importação, correção histórica ou recuperação controlada futura. Ela não autoriza inventar `startedAt` para um fato passado.

## Objeto de valor candidato

### `Weight`

`Weight` é candidato a objeto de valor porque peso é definido por valor e regras, não por identidade. A futura implementação deverá usar `BigDecimal`, nunca `double`.

A unidade é quilograma e a resolução observada é `0,01 kg`. `15,14` e `15,140` têm o mesmo significado numérico; a escala de `BigDecimal` e a quantidade de casas exibidas não representam precisão física adicional.

A interface futura deve aceitar vírgula decimal brasileira e formas equivalentes com zeros finais. Este marco não fixa duas ou três casas de apresentação. Valores incompatíveis com o incremento observado permanecem pendentes em [OQ-WGT-09](open-questions.md#oq-wgt-09), sem arredondamento silencioso ou modo escolhido. A capacidade máxima permanece aberta em [OQ-WGT-10](open-questions.md#oq-wgt-10).

## Enumerações candidatas

### `ActivityStatus`

Um estado explícito ajuda a representar intenção e transições, mas deve permanecer coerente com os campos:

- `AWAITING_RETURN_WEIGHT`: saída e `startedAt` presentes, retorno e `completedAt` ausentes e consumo indisponível;
- `COMPLETED`: saída e retorno presentes e consumo calculável;
- `CANCELLED`: condição terminal candidata, caso o cancelamento seja aprovado como estado.

Ainda será decidido se cancelamento é valor de `ActivityStatus` ou operação auditável separada. Combinações inválidas são erros do domínio, conforme o [ciclo de vida](activity-lifecycle.md).

### `CylinderStatus`

É candidato porque o cilindro vazio possui comportamento próprio que não pode ser derivado somente das atividades:

- `ACTIVE`: ainda pode ser usado se possuir peso inicial e não houver atividade sem retorno;
- `EMPTY`: preserva o histórico, não inicia atividades e deixa de aparecer nas seleções ativas.

Não se adiciona estado de descarte físico. A operação real descarta o recipiente, mas a responsabilidade atual do software termina ao registrar que ele não está mais disponível.

## Atributos simples no MVP

- Local, ordem de serviço, técnico e observações são atributos opcionais e não justificam entidades.
- `nominalNetContent` é atributo candidato do cilindro, diferente de peso bruto e sem obrigatoriedade confirmada.
- A data civil normal é derivada de `startedAt`, não um atributo editável duplicado.
- Um alerta é resultado de uma avaliação, não uma entidade por padrão.
- Histórico é uma visão de fatos preservados, não uma entidade separada.

## Limites de agregados candidatos

Um agregado reúne conceitos do domínio que precisam preservar consistência em conjunto. Sua raiz é o conceito pelo qual operações externas coordenam mudanças nesse grupo.

### Atividade de uso

`UsageActivity` é raiz candidata para proteger pesos brutos, instantes, atributos opcionais, estado e transições. Consumo pertence ao seu comportamento derivado.

### Cilindro

`Cylinder` é raiz candidata para proteger `sealNumber`, associação imutável com refrigerante, peso inicial, ciclo `ACTIVE`/`EMPTY` e informações da marcação como vazio. O conjunto crescente de atividades não deve ser parte interna do cilindro.

### Gás refrigerante

`RefrigerantGas` é uma raiz candidata porque seu cadastro e catálogo têm ciclo independente das atividades e dos cilindros.

A regra de uma atividade sem retorno por cilindro cruza esses limites. A documentação confirma a necessidade de coordenação futura, sem escolher persistência, framework ou repositório.

## Atribuição histórica do refrigerante

O cilindro nunca muda de refrigerante. Por isso, uma atividade antiga pode determinar seu gás pela associação imutável do `Cylinder`, sem snapshot em `UsageActivity` ou relação com vigência. [OQ-CYL-04](open-questions.md#oq-cyl-04) registra a decisão respondida.

## Pesos do cilindro e evidência cronológica

- `initialGrossWeight`: medição recebida antes do primeiro uso;
- `returnGrossWeight`: medição válida mais recente de uma atividade concluída;
- `finalGrossWeight`: medição registrada ao marcar o cilindro vazio;
- `lastKnownGrossWeight`: valor derivado da evidência válida mais recente entre essas fontes.

`currentGrossWeight` não deve ser armazenado como duplicação independente. Um cilindro `EMPTY` não recebe sugestão para nova saída. Transferência, manutenção, recalibração e correção manual permanecem em [OQ-WGT-06](open-questions.md#oq-wgt-06).

O cilindro não é recarregado nem reclassificado. Peso inicial, marcação como vazio e futuras correções não podem ser registrados como consumo fictício. Não há evidência para criar `CylinderWeightEvent` neste marco.

## Instantes automáticos e data civil

`startedAt`, `completedAt`, `correctedAt`, `cancelledAt` e `markedEmptyAt` provavelmente usam `Instant`. São produzidos automaticamente quando o respectivo evento ocorre.

`America/Sao_Paulo` define a exibição local e a data civil derivada. O fluxo normal não exige data ou hora informada pela pessoa e não duplica `activityDate`.

`LocalDate` permanece candidato somente para fontes excepcionais sem instante completo. Regras para importações e correções passadas ou futuras continuam em [Perguntas abertas](open-questions.md#datas-e-tempo).

## Ambiguidade da planilha legada

Uma linha com saída e sem retorno pode representar peso inicial de cilindro ou atividade sem pesagem de retorno. Uma importação futura deve exigir classificação humana e nunca criar `UsageActivity` automaticamente para um peso inicial.
