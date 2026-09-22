/* words.easy_description — 아이 눈높이로 풀어 쓴 설명 (2026-09-22).
 *
 * 원본은 팀이 따로 정리한 용어사전 100개(생활 19 · 금융 24 · 경제 45 · 투자 12)다.
 * 난이도·카테고리는 그 문서에 없어서 건드리지 않는다 — 기존 값을 그대로 둔다.
 *
 * 공유 DB 는 initdb 가 다시 돌지 않으므로 이 파일을 손으로 한 번 실행한다
 * (docs/ARCHITECTURE.md 의 스키마 관리 방식 참고). 여러 번 돌려도 안전하다.
 */

ALTER TABLE "words" ADD COLUMN IF NOT EXISTS "easy_description" VARCHAR(255) NULL;

UPDATE "words" SET "easy_description" = '돈은 물건과 서비스를 사고팔 때 서로 주고받는 거예요. 주로 동전이나 지폐, 또는 은행 계좌 속 숫자로 있어요. 돈이 있으면 갖고 싶은 걸 살 수 있고, 필요할 때를 위해 모아둘 수도 있고, 다른 사람과 물건을 편하게 주고받을 수 있어요.' WHERE "word" = '돈';
UPDATE "words" SET "easy_description" = '가격은 물건이나 서비스에 매겨진 값이에요. 주로 물건표나 메뉴판에 숫자로 적혀 있어요. 가격을 보면 얼마를 내야 살 수 있는지 미리 알 수 있고, 내가 가진 돈으로 무엇을 살지 정할 수 있고, 물건이 얼마나 귀하거나 흔한지도 짐작할 수 있어요.' WHERE "word" = '가격';
UPDATE "words" SET "easy_description" = '소비는 내가 가진 돈을 내고 물건이나 서비스를 얻어서 쓰는 거예요. 주로 가게나 온라인에서 물건을 살 때 이루어져요. 소비를 하면 갖고 싶던 물건을 가질 수 있고, 꼭 필요한 것만 골라 사면 돈을 아낄 수 있고, 많은 사람이 소비를 하면 물건을 만드는 사람들의 일자리도 늘어날 수 있어요.' WHERE "word" = '소비';
UPDATE "words" SET "easy_description" = '소비자는 돈을 내고 물건이나 서비스를 사서 쓰는 사람이에요. 주로 가게에서 물건을 고르는 손님을 가리켜요. 소비자는 자신에게 필요한 물건을 직접 고를 수 있고, 여러 가게의 가격을 비교해 더 좋은 걸 선택할 수 있고, 소비자가 많이 찾는 물건일수록 더 많이 만들어지게 돼요.' WHERE "word" = '소비자';
UPDATE "words" SET "easy_description" = '생산은 사람들에게 필요한 물건이나 서비스를 만들어 내는 거예요. 주로 공장이나 농장, 가게에서 이루어져요. 생산을 하면 사람들이 필요로 하는 걸 얻을 수 있고, 만든 사람은 그 대가로 돈을 벌 수 있고, 여러 물건이 생산되면서 우리 생활이 더 편리해져요.' WHERE "word" = '생산';
UPDATE "words" SET "easy_description" = '생산자는 물건이나 서비스를 직접 만들어서 파는 사람이나 회사예요. 주로 공장이나 농장, 회사가 이 역할을 해요. 생산자는 물건을 팔아서 돈을 벌 수 있고, 사람들에게 필요한 걸 만들어 도움을 줄 수 있고, 좋은 물건을 만들수록 더 많은 소비자의 선택을 받게 돼요.' WHERE "word" = '생산자';
UPDATE "words" SET "easy_description" = '수입은 내가 일을 하거나 물건을 팔아서 받는 돈이에요. 용돈을 받거나 아르바이트를 해서 돈을 버는 것도 수입이에요. 수입이 있으면 필요한 물건을 살 수 있고, 저축을 할 수도 있어요.' WHERE "word" = '수입';
UPDATE "words" SET "easy_description" = '지출은 물건이나 서비스를 사느라 내가 쓰는 돈이에요. 간식을 사 먹거나 준비물을 사는 게 다 지출이에요. 지출을 잘 살펴보면 돈을 어디에 많이 쓰는지 알 수 있고, 필요 없는 지출을 줄일 수도 있어요.' WHERE "word" = '지출';
UPDATE "words" SET "easy_description" = '용돈은 부모님께 정기적으로 받아서 내가 쓰고 싶은 곳에 쓸 수 있는 돈이에요. 용돈이 있으면 갖고 싶은 걸 직접 골라 살 수 있고, 계획을 세워 아껴 쓰는 연습도 할 수 있어요.' WHERE "word" = '용돈';
UPDATE "words" SET "easy_description" = '예산은 앞으로 들어올 돈과 써야 할 돈을 미리 정해두는 계획이에요. 예산을 세우면 돈을 어디에 얼마나 쓸지 미리 정할 수 있고, 필요 없는 곳에 돈을 쓰지 않게 되고, 하고 싶은 일을 위해 돈을 모을 수도 있어요.' WHERE "word" = '예산';
UPDATE "words" SET "easy_description" = '가계부는 들어온 돈과 나간 돈을 적어두는 기록장이에요. 가계부를 쓰면 내가 돈을 얼마나 벌고 썼는지 한눈에 알 수 있고, 어디에 돈을 많이 쓰는지 확인할 수 있고, 다음에 돈을 더 잘 쓰는 데도 도움이 돼요.' WHERE "word" = '가계부';
UPDATE "words" SET "easy_description" = '필요는 생활하려면 꼭 있어야 하는 것이에요. 밥, 옷, 학용품처럼 없으면 생활하기 어려운 것들이 필요에 해당돼요. 필요한 것을 먼저 채우면 생활이 안정되고, 돈을 계획적으로 쓸 수 있어요.' WHERE "word" = '필요';
UPDATE "words" SET "easy_description" = '욕구는 꼭 있어야 하는 건 아니지만 갖고 싶거나 하고 싶은 마음이에요. 새로운 장난감이나 게임이 하고 싶은 마음이 욕구예요. 욕구를 다 채우려 하면 돈이 부족할 수 있어서, 필요와 욕구를 구분하는 게 중요해요.' WHERE "word" = '욕구';
UPDATE "words" SET "easy_description" = '선택은 여러 가지 중에서 하나를 고르는 거예요. 용돈으로 과자를 살지 저금을 할지 정하는 것도 선택이에요. 좋은 선택을 하면 가진 돈과 시간을 더 알차게 쓸 수 있어요.' WHERE "word" = '선택';
UPDATE "words" SET "easy_description" = '기회비용은 하나를 선택하면서 포기하게 되는 것 중 가장 아까운 걸 말해요. 장난감을 사면 그 돈으로 살 수 있었던 다른 것을 포기하는 거예요. 기회비용을 생각하면 더 신중하게 선택할 수 있어요.' WHERE "word" = '기회비용';
UPDATE "words" SET "easy_description" = '할인은 원래 가격보다 물건을 더 싸게 파는 거예요. 세일 기간에 물건을 사면 같은 돈으로 더 많이 살 수 있어요. 할인을 잘 이용하면 돈을 아낄 수 있어요.' WHERE "word" = '할인';
UPDATE "words" SET "easy_description" = '현금은 동전이나 지폐처럼 손으로 직접 주고받을 수 있는 돈이에요. 현금이 있으면 카드나 휴대폰이 없어도 바로 물건을 살 수 있어요.' WHERE "word" = '현금';
UPDATE "words" SET "easy_description" = '영수증은 물건을 사고 돈을 냈다는 걸 적어주는 종이예요. 영수증을 받아두면 무엇을 얼마에 샀는지 나중에 확인할 수 있고, 물건에 문제가 있을 때 환불받을 때도 필요해요.' WHERE "word" = '영수증';
UPDATE "words" SET "easy_description" = '환불은 산 물건을 다시 돌려주고 냈던 돈을 되돌려받는 거예요. 물건이 마음에 들지 않거나 문제가 있을 때 환불을 받을 수 있고, 이때 영수증이 있으면 더 쉽게 환불받을 수 있어요.' WHERE "word" = '환불';
UPDATE "words" SET "easy_description" = '저축은 지금 당장 쓰지 않고 나중을 위해 돈을 모아두는 거예요. 저축을 하면 미래에 돈이 필요할 때 사용할 수 있고, 꼭 필요한 것만 사게 되어 낭비를 줄일 수 있고, 은행이 그 돈을 필요한 사람이나 기업에 빌려줄 수도 있어요.' WHERE "word" = '저축';
UPDATE "words" SET "easy_description" = '은행은 사람들의 돈을 안전하게 맡아주고, 돈이 필요한 사람이나 회사에 빌려주는 곳이에요. 은행에 돈을 맡기면 잃어버릴 걱정 없이 돈을 보관할 수 있고, 이자도 받을 수 있어요.' WHERE "word" = '은행';
UPDATE "words" SET "easy_description" = '예금은 은행에 돈을 맡겨두는 거예요. 예금을 하면 돈을 안전하게 보관할 수 있고, 필요할 때 언제든 찾을 수 있고, 맡겨둔 동안 이자도 받을 수 있어요.' WHERE "word" = '예금';
UPDATE "words" SET "easy_description" = '적금은 정해진 기간 동안 매달 일정한 돈을 은행에 꾸준히 넣는 거예요. 적금을 들면 조금씩 모은 돈이 목돈이 되고, 정해진 기간이 지나면 원금과 이자를 함께 받을 수 있어요.' WHERE "word" = '적금';
UPDATE "words" SET "easy_description" = '통장은 은행에 맡긴 돈이 얼마나 들어오고 나갔는지 기록해서 보여주는 거예요. 통장을 보면 지금 내 돈이 얼마나 남아있는지 바로 확인할 수 있어요.' WHERE "word" = '통장';
UPDATE "words" SET "easy_description" = '계좌는 은행에서 내 돈을 관리하기 위해 만들어주는 나만의 번호이자 공간이에요. 계좌가 있으면 돈을 안전하게 넣고 뺄 수 있고, 다른 사람에게 돈을 보내거나 받을 수도 있어요.' WHERE "word" = '계좌';
UPDATE "words" SET "easy_description" = '잔액은 지금 계좌나 지갑에 남아 있는 돈이에요. 잔액을 확인하면 내가 얼마나 쓸 수 있는 돈이 남았는지 알 수 있어요.' WHERE "word" = '잔액';
UPDATE "words" SET "easy_description" = '입금은 은행 계좌에 돈을 넣는 거예요. 용돈을 받아 계좌에 넣으면 그것도 입금이에요. 입금을 하면 돈을 잃어버리지 않고 안전하게 모을 수 있어요.' WHERE "word" = '입금';
UPDATE "words" SET "easy_description" = '출금은 은행 계좌에 있던 돈을 꺼내서 쓰는 거예요. 필요한 만큼만 출금하면 계획한 대로 돈을 쓸 수 있어요.' WHERE "word" = '출금';
UPDATE "words" SET "easy_description" = '이체는 내 계좌에 있는 돈을 다른 사람의 계좌로 옮겨주는 거예요. 이체를 하면 직접 만나지 않고도 돈을 주고받을 수 있어서 편리해요.' WHERE "word" = '이체';
UPDATE "words" SET "easy_description" = '이자는 돈을 맡기거나 빌릴 때 원래 돈에 더 붙여서 주고받는 돈이에요. 돈을 저축하면 은행에서 이자를 받을 수 있고, 반대로 돈을 빌리면 이자를 더해서 갚아야 해요.' WHERE "word" = '이자';
UPDATE "words" SET "easy_description" = '금리는 원래 돈에 이자가 얼마나 붙는지를 퍼센트(%)로 보여주는 숫자예요. 금리가 높으면 저축했을 때 이자를 더 많이 받을 수 있고, 돈을 빌렸을 때는 갚아야 할 이자도 더 많아져요.' WHERE "word" = '금리';
UPDATE "words" SET "easy_description" = '원금은 처음에 맡기거나 빌린 돈 자체를 말해요. 이자는 이 원금에 더해서 주고받는 돈이에요.' WHERE "word" = '원금';
UPDATE "words" SET "easy_description" = '대출은 은행이나 다른 곳에서 돈을 빌리는 거예요. 대출을 받으면 지금 필요한 돈을 먼저 쓸 수 있지만, 나중에 이자를 더해서 갚아야 해요.' WHERE "word" = '대출';
UPDATE "words" SET "easy_description" = '빚은 다른 사람이나 은행에게 갚아야 할 돈이에요. 빚이 많으면 갚는 데 부담이 될 수 있어서, 돈을 빌릴 때는 갚을 수 있는 만큼만 빌리는 게 중요해요.' WHERE "word" = '빚';
UPDATE "words" SET "easy_description" = '신용은 내가 돈을 잘 갚고 약속을 잘 지킬 거라고 다른 사람이 믿어주는 정도예요. 신용이 좋으면 돈을 빌리기 쉬워지고, 여러 혜택도 받을 수 있어요.' WHERE "word" = '신용';
UPDATE "words" SET "easy_description" = '신용카드는 지금 당장 돈이 없어도 먼저 물건을 사고, 나중에 정해진 날짜에 그 돈을 갚는 카드예요. 편리하지만 계획 없이 쓰면 갚아야 할 돈이 많아질 수 있어요.' WHERE "word" = '신용카드';
UPDATE "words" SET "easy_description" = '체크카드는 결제하는 순간 내 은행 계좌에서 바로 돈이 빠져나가는 카드예요. 계좌에 있는 돈만큼만 쓸 수 있어서 계획적으로 돈을 관리하기 좋아요.' WHERE "word" = '체크카드';
UPDATE "words" SET "easy_description" = '전자결제는 현금 없이 카드나 휴대전화, 인터넷으로 돈을 내는 방법이에요. 전자결제를 하면 지갑이 없어도 편리하게 결제할 수 있어요.' WHERE "word" = '전자결제';
UPDATE "words" SET "easy_description" = '보험은 사고나 아픈 일처럼 갑자기 생길 수 있는 큰 어려움에 미리 대비하는 제도예요. 평소에 조금씩 돈을 내두면, 정말 어려운 일이 생겼을 때 도움을 받을 수 있어요.' WHERE "word" = '보험';
UPDATE "words" SET "easy_description" = '사기는 다른 사람을 속여서 돈이나 물건을 빼앗는 나쁜 행동이에요. 사기를 당하지 않으려면 낯선 사람의 말을 그대로 믿지 않고 조심하는 게 중요해요.' WHERE "word" = '사기';
UPDATE "words" SET "easy_description" = '개인정보는 이름, 주소, 전화번호, 계좌번호처럼 나를 알아볼 수 있게 해주는 정보예요. 개인정보가 다른 사람에게 함부로 알려지면 위험할 수 있어서 소중히 지켜야 해요.' WHERE "word" = '개인정보';
UPDATE "words" SET "easy_description" = '금융사기는 사람을 속여서 돈이나 계좌 정보를 빼앗는 범죄예요. 모르는 사람이 돈이나 개인정보를 알려달라고 하면 절대 알려주지 말고 어른에게 먼저 물어봐야 해요.' WHERE "word" = '금융사기';
UPDATE "words" SET "easy_description" = '계약은 두 사람이나 기관이 서로 지키기로 한 약속이에요. 계약을 하면 서로 정한 내용을 꼭 지켜야 하고, 어기면 문제가 생길 수 있어요.' WHERE "word" = '계약';
UPDATE "words" SET "easy_description" = '시장은 사람들이 물건과 서비스를 사고파는 곳이나 그런 관계를 말해요. 마트나 온라인 쇼핑몰도 모두 시장의 한 종류예요.' WHERE "word" = '시장';
UPDATE "words" SET "easy_description" = '수요는 사람들이 어떤 물건을 얼마나 사고 싶어 하는지를 말해요. 사고 싶어 하는 사람이 많을수록 수요가 크다고 해요.' WHERE "word" = '수요';
UPDATE "words" SET "easy_description" = '공급은 파는 사람이 시장에 물건을 얼마나 내놓는지를 말해요. 공급이 많으면 살 수 있는 물건도 많아져요.' WHERE "word" = '공급';
UPDATE "words" SET "easy_description" = '수요와 공급은 사려는 사람의 양과 팔려는 사람의 양이 만나는 관계예요. 사려는 사람이 많고 물건이 적으면 가격이 오르고, 반대면 가격이 내려가요.' WHERE "word" = '수요와 공급';
UPDATE "words" SET "easy_description" = '물가는 여러 가지 물건과 서비스의 가격을 전체적으로 살펴본 수준이에요. 물가를 보면 요즘 물건 값이 전반적으로 비싼지 싼지 알 수 있어요.' WHERE "word" = '물가';
UPDATE "words" SET "easy_description" = '물가 상승은 여러 물건과 서비스의 가격이 전체적으로 오르는 거예요. 물가가 오르면 같은 돈으로 살 수 있는 물건의 양이 줄어들어요.' WHERE "word" = '물가 상승';
UPDATE "words" SET "easy_description" = '원가는 물건을 만드는 데 실제로 들어간 돈이에요. 원가보다 더 비싸게 팔아야 만든 사람이 이익을 남길 수 있어요.' WHERE "word" = '원가';
UPDATE "words" SET "easy_description" = '이익은 물건을 팔아서 번 돈에서 만드는 데 쓴 비용을 빼고 남은 돈이에요. 이익이 나야 사업을 계속할 수 있어요.' WHERE "word" = '이익';
UPDATE "words" SET "easy_description" = '손해는 번 돈보다 쓴 돈이 더 많아서 돈을 잃은 상태예요. 손해가 계속되면 사업을 이어가기 어려워질 수 있어요.' WHERE "word" = '손해';
UPDATE "words" SET "easy_description" = '경쟁은 여러 판매자가 손님의 선택을 받기 위해 가격이나 품질로 서로 겨루는 거예요. 경쟁이 있으면 더 좋은 물건을 더 싸게 살 수 있어요.' WHERE "word" = '경쟁';
UPDATE "words" SET "easy_description" = '독점은 한 회사가 어떤 물건이나 서비스를 거의 혼자서 파는 상태예요. 독점이 되면 경쟁이 없어서 가격이 비싸질 수 있어요.' WHERE "word" = '독점';
UPDATE "words" SET "easy_description" = '기업은 물건이나 서비스를 만들어 팔면서 경제 활동을 하는 조직이에요. 우리 주변의 회사들이 대부분 기업이에요.' WHERE "word" = '기업';
UPDATE "words" SET "easy_description" = '직업은 생활에 필요한 돈을 벌기 위해 하는 일이에요. 선생님, 의사, 요리사처럼 각자 잘하는 일을 직업으로 삼아요.' WHERE "word" = '직업';
UPDATE "words" SET "easy_description" = '임금은 일을 한 대가로 받는 돈이에요. 임금을 받으면 그 돈으로 필요한 것을 사거나 저축할 수 있어요.' WHERE "word" = '임금';
UPDATE "words" SET "easy_description" = '매출은 가게나 회사가 물건을 팔아서 받은 돈을 모두 더한 금액이에요. 매출이 많다고 꼭 이익이 큰 건 아니고, 비용을 빼야 진짜 이익을 알 수 있어요.' WHERE "word" = '매출';
UPDATE "words" SET "easy_description" = '비용은 물건을 만들거나 가게를 운영하는 데 들어가는 돈이에요. 재료값이나 월세 같은 것들이 비용에 들어가요.' WHERE "word" = '비용';
UPDATE "words" SET "easy_description" = '수익은 어떤 경제 활동을 해서 벌어들인 돈이나 이익을 말해요. 수익이 많을수록 더 많은 돈을 모을 수 있어요.' WHERE "word" = '수익';
UPDATE "words" SET "easy_description" = '창업은 새로운 가게나 회사를 만들어 사업을 처음 시작하는 거예요. 창업을 하면 자신만의 아이디어로 돈을 벌 수 있지만, 그만큼 책임과 위험도 따라와요.' WHERE "word" = '창업';
UPDATE "words" SET "easy_description" = '사업은 물건이나 서비스를 만들어 팔면서 돈을 버는 활동이에요. 사업이 잘되면 큰 수입을 얻을 수 있지만, 반대로 손해를 볼 수도 있어요.' WHERE "word" = '사업';
UPDATE "words" SET "easy_description" = '고용은 회사나 기관이 사람을 뽑아서 일을 맡기는 거예요. 고용이 늘어나면 더 많은 사람이 일자리를 갖게 돼요.' WHERE "word" = '고용';
UPDATE "words" SET "easy_description" = '실업은 일하고 싶은 마음과 능력이 있는데도 일자리를 구하지 못한 상태예요. 실업이 늘어나면 사회 전체적으로도 어려움이 커질 수 있어요.' WHERE "word" = '실업';
UPDATE "words" SET "easy_description" = '생산성은 같은 시간이나 돈을 들여서 얼마나 많은 결과물을 만들어내는지를 보여주는 거예요. 생산성이 높아지면 더 적은 노력으로 더 많은 것을 만들 수 있어요.' WHERE "word" = '생산성';
UPDATE "words" SET "easy_description" = '경제는 사람들이 필요한 것을 만들고, 사고팔고, 돈을 쓰는 모든 활동을 말해요. 우리가 물건을 사고, 일을 하고, 돈을 모으는 것도 모두 경제 활동이에요.' WHERE "word" = '경제';
UPDATE "words" SET "easy_description" = '경제 성장은 한 나라가 만들어내는 물건과 서비스가 늘어나서 나라 경제가 커지는 거예요. 경제가 성장하면 사람들의 생활도 전반적으로 더 풍요로워질 수 있어요.' WHERE "word" = '경제 성장';
UPDATE "words" SET "easy_description" = '경기는 나라의 경제가 활발한지 조용한지를 보여주는 상태예요. 경기가 좋으면 사람들이 소비도 늘고 일자리도 많아져요.' WHERE "word" = '경기';
UPDATE "words" SET "easy_description" = '호황은 경제 활동이 활발해져서 소비와 생산이 함께 늘어나는 시기예요. 호황일 때는 일자리도 많아지고 사람들의 지갑도 넉넉해지는 경우가 많아요.' WHERE "word" = '호황';
UPDATE "words" SET "easy_description" = '불황은 경제 활동이 줄어들어서 소비와 생산이 함께 움츠러드는 시기예요. 불황일 때는 일자리를 구하기가 더 어려워질 수 있어요.' WHERE "word" = '불황';
UPDATE "words" SET "easy_description" = '국내총생산(GDP)은 한 나라 안에서 일정 기간 동안 새로 만들어진 물건과 서비스의 가치를 모두 더한 숫자예요. GDP가 크면 그 나라의 경제 규모가 크다는 뜻이에요.' WHERE "word" = '국내총생산(GDP)';
UPDATE "words" SET "easy_description" = '세금은 나라와 지역이 도로나 학교 같은 공공서비스를 운영하려고 국민들에게 걷는 돈이에요. 우리가 낸 세금은 다시 여러 공공서비스로 우리에게 돌아와요.' WHERE "word" = '세금';
UPDATE "words" SET "easy_description" = '정부는 나라의 살림을 맡아 운영하고 국민을 위한 여러 정책을 만드는 기관이에요. 정부는 세금을 걷어서 도로, 학교, 병원 같은 것들을 운영해요.' WHERE "word" = '정부';
UPDATE "words" SET "easy_description" = '복지는 국가나 사회가 사람들이 더 안정적으로 살 수 있도록 도와주는 제도예요. 어려운 상황에 놓인 사람들도 복지 덕분에 도움을 받을 수 있어요.' WHERE "word" = '복지';
UPDATE "words" SET "easy_description" = '공공재는 도로나 공원처럼 여러 사람이 함께 사용할 수 있는 것들이에요. 공공재는 한 사람이 쓴다고 다른 사람이 못 쓰게 되지 않아요.' WHERE "word" = '공공재';
UPDATE "words" SET "easy_description" = '국가 예산은 정부가 일정 기간 동안 세금을 얼마나 걷고 어디에 쓸지 미리 세운 계획이에요. 예산이 잘 세워져야 나라 살림도 안정적으로 운영돼요.' WHERE "word" = '국가 예산';
UPDATE "words" SET "easy_description" = '재정은 정부가 세금 등으로 돈을 모으고 그 돈을 쓰는 모든 활동을 말해요. 재정이 튼튼해야 나라가 여러 정책을 안정적으로 펼칠 수 있어요.' WHERE "word" = '재정';
UPDATE "words" SET "easy_description" = '중앙은행은 나라의 돈과 금리를 관리하면서 경제가 안정되도록 돕는 특별한 은행이에요. 우리가 흔히 가는 은행들과는 다르게, 나라 전체의 돈 흐름을 관리해요.' WHERE "word" = '중앙은행';
UPDATE "words" SET "easy_description" = '한국은행은 우리나라의 중앙은행으로, 돈의 가치와 물가를 안정시키는 일을 해요. 우리가 쓰는 돈을 만들어내는 곳이기도 해요.' WHERE "word" = '한국은행';
UPDATE "words" SET "easy_description" = '기준금리는 한국은행이 경제 상황을 살펴보고 정하는 기준이 되는 금리예요. 기준금리가 바뀌면 은행의 예금과 대출 금리도 함께 영향을 받아요.' WHERE "word" = '기준금리';
UPDATE "words" SET "easy_description" = '무역은 나라와 나라가 서로 물건이나 서비스를 사고파는 활동이에요. 무역 덕분에 우리나라에 없는 물건도 다른 나라에서 가져올 수 있어요.' WHERE "word" = '무역';
UPDATE "words" SET "easy_description" = '수출은 우리나라에서 만든 물건이나 서비스를 다른 나라에 파는 거예요. 수출이 늘어나면 우리나라가 버는 돈도 늘어나요.' WHERE "word" = '수출';
UPDATE "words" SET "easy_description" = '수입은 다른 나라에서 만든 물건이나 서비스를 우리나라가 사 오는 거예요. 수입 덕분에 우리나라에서 만들지 못하는 것도 쓸 수 있어요.' WHERE "word" = '수입(무역)';
UPDATE "words" SET "easy_description" = '환율은 다른 나라의 돈으로 바꿀 때 적용되는 비율이에요. 환율에 따라 같은 돈이라도 바꿀 수 있는 외국 돈의 양이 달라져요.' WHERE "word" = '환율';
UPDATE "words" SET "easy_description" = '외화는 달러나 엔처럼 다른 나라에서 쓰는 돈이에요. 다른 나라를 여행하거나 물건을 살 때는 그 나라의 외화가 필요해요.' WHERE "word" = '외화';
UPDATE "words" SET "easy_description" = '관세는 다른 나라에서 들어오는 물건에 정부가 매기는 세금이에요. 관세가 붙으면 외국 물건의 가격이 조금 더 비싸질 수 있어요.' WHERE "word" = '관세';
UPDATE "words" SET "easy_description" = '환전은 우리나라 돈을 외국 돈으로 바꾸거나, 반대로 바꾸는 거예요. 해외여행을 가기 전에 환전을 해두면 그 나라에서 바로 돈을 쓸 수 있어요.' WHERE "word" = '환전';
UPDATE "words" SET "easy_description" = '원자재는 물건을 만들 때 기본 재료로 쓰이는 석유, 철, 곡물 같은 자원이에요. 원자재 값이 오르면 그것으로 만든 물건의 가격도 오를 수 있어요.' WHERE "word" = '원자재';
UPDATE "words" SET "easy_description" = '투자는 앞으로 더 큰 이익을 기대하며 돈이나 시간을 쓰는 거예요. 투자를 하면 돈이 불어날 수도 있지만, 반대로 줄어들 수도 있어요.' WHERE "word" = '투자';
UPDATE "words" SET "easy_description" = '자산은 돈으로 값을 매길 수 있는, 내가 가진 재산을 말해요. 통장에 있는 돈, 집, 주식 같은 것들이 모두 자산이 될 수 있어요.' WHERE "word" = '자산';
UPDATE "words" SET "easy_description" = '주식은 회사의 일부를 나누어 가진 권리를 보여주는 증서예요. 주식을 가지면 그 회사의 주인 중 한 명이 되는 셈이에요.' WHERE "word" = '주식';
UPDATE "words" SET "easy_description" = '주주는 회사의 주식을 가지고 있는 사람이나 기관이에요. 주주는 회사가 잘되면 이익을 나눠 받을 수 있어요.' WHERE "word" = '주주';
UPDATE "words" SET "easy_description" = '주가는 주식 한 개가 시장에서 사고팔리는 가격이에요. 회사가 잘되면 주가가 오르고, 어려워지면 주가가 내려갈 수 있어요.' WHERE "word" = '주가';
UPDATE "words" SET "easy_description" = '배당은 회사가 번 이익의 일부를 주주들에게 나눠주는 거예요. 주식을 가진 사람은 배당을 통해 회사의 이익을 함께 받을 수 있어요.' WHERE "word" = '배당';
UPDATE "words" SET "easy_description" = '채권은 정부나 회사에 돈을 빌려주고, 나중에 원금과 이자를 돌려받기로 한 약속이에요. 채권을 사면 돈을 빌려준 대가로 이자를 받을 수 있어요.' WHERE "word" = '채권';
UPDATE "words" SET "easy_description" = '펀드는 여러 사람의 돈을 모아서 전문가가 대신 여러 곳에 나누어 투자해주는 상품이에요. 혼자 투자하기 어려운 사람도 펀드를 통해 투자에 참여할 수 있어요.' WHERE "word" = '펀드';
UPDATE "words" SET "easy_description" = '수익률은 투자한 돈에 비해 얼마나 이익이나 손해를 봤는지를 비율(%)로 보여주는 숫자예요. 수익률이 높을수록 투자한 돈이 크게 불어났다는 뜻이에요.' WHERE "word" = '수익률';
UPDATE "words" SET "easy_description" = '위험은 투자한 돈의 가치가 줄어들거나 생각한 대로 결과가 나오지 않을 가능성을 말해요. 큰 이익을 기대할수록 위험도 커지는 경우가 많아요.' WHERE "word" = '위험';
UPDATE "words" SET "easy_description" = '분산투자는 위험을 줄이려고 돈을 한 곳에 몰아넣지 않고 여러 자산에 나누어 투자하는 방법이에요. 한 곳에서 손해를 보더라도 다른 곳에서 만회할 수 있어요.' WHERE "word" = '분산투자';
UPDATE "words" SET "easy_description" = '원금 손실은 투자한 돈이 줄어들어서 처음 넣었던 금액보다 적어지는 거예요. 투자를 할 때는 원금 손실이 생길 수도 있다는 걸 미리 알고 신중하게 결정해야 해요.' WHERE "word" = '원금 손실';

/* 채우지 못한 단어 확인 — 이 목록이 비어야 끝이다.
   (용어사전에 없는 단어가 DB 에 남아 있으면 여기 나온다) */
-- SELECT word_id, word FROM words WHERE easy_description IS NULL ORDER BY word_id;
