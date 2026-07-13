# Modelo de domínio candidato

Este documento descreve conceitos candidatos, não classes Java prontas. O objetivo é tornar responsabilidades e relações compreensíveis antes de escolher detalhes de implementação no Milestone 2B.

As regras obrigatórias estão em [Regras de negócio](business-rules.md), e os estados estão em [Ciclo de vida da atividade](activity-lifecycle.md).

## Entidades candidatas

### `RefrigerantGas`

É candidato a entidade porque um tipo de refrigerante possui identidade e informações próprias, pode existir antes de qualquer cilindro e pode ser associado a vários cilindros. O cadastro mínimo pertence ao MVP; o catálogo técnico ampliado é separado da operação.

Ainda não estão confirmados o identificador, os campos mínimos nem as fontes confiáveis do catálogo.

### `Cylinder`

É candidato a entidade porque um recipiente físico precisa ser distinguido dos demais e participa de atividades ao longo do tempo. Sua identidade operacional e a forma histórica de associação com o refrigerante dependem da planilha e da operação real.

Disponibilidade não deve ser armazenada como informação independente se puder ser obtida com segurança pela existência de atividade aberta.

### `UsageActivity`

É candidato a entidade porque representa um fato operacional identificável, muda de estado e precisa preservar sua história em correções e cancelamentos.

Uma `UsageActivity` pode ser criada concluída ou aguardando retorno. Não serão criados conceitos separados `OpenActivity` e `CompletedActivity`: a diferença pertence ao ciclo de vida da mesma atividade.

## Objeto de valor candidato

### `Weight`

`Weight` é candidato a objeto de valor porque peso é definido por seu valor e regras, não por uma identidade própria. Uma futura implementação poderá concentrar comparações e impedir valores inválidos.

A representação provável do número é `BigDecimal`, não `double`. A decisão final depende de unidade, precisão da balança, escala, arredondamento, peso bruto ou líquido e tratamento de tara. Enquanto essas perguntas estiverem abertas, este documento não define construtor, escala ou tolerância.

## Enumeração candidata

### `ActivityStatus`

Um estado explícito ajuda a representar intenção e transições, mas deve permanecer coerente com os campos:

- `AWAITING_RETURN`: saída presente, retorno ausente e consumo indisponível;
- `COMPLETED`: saída e retorno presentes e consumo calculável;
- `CANCELLED`: condição terminal candidata, caso o cancelamento seja aprovado como estado.

Ainda será decidido se cancelamento é um valor de `ActivityStatus` ou uma operação auditável separada. Combinações inválidas são erros do domínio, conforme o [ciclo de vida](activity-lifecycle.md).

## Atributos simples no MVP

- O local permanece texto porque não há ciclo de vida independente confirmado.
- Ordem de serviço, técnico, observações e detalhes adicionais não justificam entidades.
- Datas operacionais são valores associados à atividade, não entidades.
- Um alerta é resultado de uma avaliação, não uma entidade por padrão.
- Histórico é uma visão de fatos preservados, não uma entidade separada.

## Limites de agregados candidatos

Um agregado reúne conceitos do domínio que precisam preservar consistência em conjunto. Sua raiz é o conceito pelo qual operações externas coordenam mudanças nesse grupo.

### Atividade de uso

`UsageActivity` é uma raiz candidata para proteger seus pesos, local, datas operacionais, estado e transições. Consumo pertence ao seu comportamento derivado.

### Cilindro

`Cylinder` é uma raiz candidata para proteger sua identidade e a relação permitida com refrigerante. O conjunto completo de atividades não deve ser carregado como parte interna crescente do cilindro.

### Gás refrigerante

`RefrigerantGas` é uma raiz candidata porque seu cadastro e catálogo têm ciclo independente das atividades e dos cilindros.

A regra envolvendo uma atividade aberta e um cilindro cruza esses limites. A documentação apenas identifica essa necessidade de coordenação futura; não escolhe mecanismo de armazenamento ou framework.

## Atribuição histórica do refrigerante

Não é seguro afirmar que o gás de uma atividade antiga sempre pode ser obtido da associação atual do cilindro. Se essa associação mudar, a consulta poderia mostrar um gás diferente daquele usado na saída e reescrever a história aparente.

As opções em avaliação são:

1. o cilindro nunca muda de tipo de refrigerante;
2. `UsageActivity` preserva uma cópia histórica do refrigerante válido na saída;
3. a relação entre cilindro e refrigerante possui histórico com períodos de vigência.

Nenhuma opção foi escolhida. [OQ-CYL-04](open-questions.md#oq-cyl-04) bloqueia a implementação final dessa relação no Milestone 2B.

## Último peso conhecido e eventos fora do consumo

O último retorno pode não ser a fonte universal da próxima sugestão de saída. O peso também pode mudar por cadastro inicial, recarga, transferência, manutenção, recalibração da balança, correção manual, substituição ou desativação.

Esses fatos não devem ser registrados como atividades de consumo fictícias. Um conceito futuro como `CylinderWeightEvent` só será considerado se a operação comprovar a necessidade; não é entidade aprovada neste modelo.

As perguntas [OQ-WGT-05](open-questions.md#oq-wgt-05) e [OQ-WGT-06](open-questions.md#oq-wgt-06) bloqueiam a definição da origem de “último peso conhecido”.

## Datas operacionais e instantes técnicos

Datas informadas por pessoas provavelmente usarão `LocalDate`. Instantes automáticos de auditoria provavelmente usarão `Instant`. Esta é uma direção de modelagem, não implementação.

Devem ser distinguidos:

- data de saída;
- possível data de retorno;
- instante de criação;
- instante de conclusão;
- instante de correção;
- instante de cancelamento.

Nem todos se tornam campos obrigatórios visíveis. As perguntas sobre datas e fuso estão consolidadas em [Perguntas abertas](open-questions.md#datas-e-tempo).
