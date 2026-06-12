# -*- coding: utf-8 -*-
from fpdf import FPDF

BLUE = (43, 92, 168)
GREY = (110, 110, 110)
DARK = (26, 26, 26)
RED = (192, 57, 43)
LIGHTBG = (240, 244, 250)
QUOTEBG = (247, 247, 242)
GOLD = (201, 162, 39)
TIPBG = (255, 248, 225)

class PDF(FPDF):
    def header(self):
        pass
    def footer(self):
        self.set_y(-12)
        self.set_font("Noto", "", 8)
        self.set_text_color(*GREY)
        self.cell(0, 8, f"임상심리학 모의고사  -  {self.page_no()}", align="C")

pdf = PDF(orientation="P", unit="mm", format="A4")
pdf.add_font("Noto", "", "/usr/share/fonts/truetype/nanum/NanumGothic.ttf")
pdf.add_font("Noto", "B", "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf")
pdf.set_auto_page_break(True, margin=15)
pdf.set_margins(15, 14, 15)
pdf.add_page()
EPW = pdf.epw  # effective page width

def title_block():
    pdf.set_font("Noto", "B", 17)
    pdf.set_text_color(*BLUE)
    pdf.cell(0, 9, "임상심리학 모의고사 (기출 형식)", align="C", new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Noto", "", 9.5)
    pdf.set_text_color(*GREY)
    pdf.cell(0, 6, "시험 범위: 7 · 9 · 10 · 11 · 13주차   |   이준득 교수", align="C", new_x="LMARGIN", new_y="NEXT")
    # underline
    y = pdf.get_y() + 1
    pdf.set_draw_color(*BLUE); pdf.set_line_width(0.6)
    pdf.line(15, y, 15 + EPW, y)
    pdf.ln(4)
    # info bar
    pdf.set_fill_color(*LIGHTBG); pdf.set_text_color(*DARK)
    pdf.set_font("Noto", "", 8.5)
    pdf.cell(EPW/3, 7, "  객관식 4지선다 · 총 40문항", fill=True)
    pdf.cell(EPW/3, 7, "문항당 2.5점 (총 100점)", align="C", fill=True)
    pdf.cell(EPW/3, 7, "이름: ____________  ", align="R", fill=True, new_x="LMARGIN", new_y="NEXT")
    pdf.ln(4)

def section(text):
    if pdf.get_y() > 250:
        pdf.add_page()
    pdf.ln(1)
    pdf.set_fill_color(*BLUE); pdf.set_text_color(255,255,255)
    pdf.set_font("Noto", "B", 11)
    pdf.cell(0, 8, "  " + text, fill=True, new_x="LMARGIN", new_y="NEXT")
    pdf.ln(2.5)

def question(num, text, opts, quote=None):
    # estimate height needed; if near bottom, page break to avoid splitting
    needed = 7 + (8 if quote else 0) + 14
    if pdf.get_y() + needed > 278:
        pdf.add_page()
    pdf.set_text_color(*DARK)
    # question line: bold number + score + text
    pdf.set_font("Noto", "B", 10.5)
    numw = pdf.get_string_width(f"{num}. ") + 1
    pdf.cell(numw, 6, f"{num}.")
    pdf.set_font("Noto", "", 8)
    pdf.set_text_color(*GREY)
    sw = pdf.get_string_width("[2.5점] ") + 1
    pdf.cell(sw, 6, "[2.5점]")
    pdf.set_text_color(*DARK)
    pdf.set_font("Noto", "", 10.5)
    pdf.multi_cell(EPW - numw - sw, 6, text, new_x="LMARGIN", new_y="NEXT")
    if quote:
        pdf.ln(0.5)
        pdf.set_fill_color(*QUOTEBG)
        pdf.set_font("Noto", "", 9.5)
        pdf.set_text_color(60,60,60)
        x0 = pdf.get_x()
        # gold left bar
        ystart = pdf.get_y()
        pdf.set_x(20)
        pdf.multi_cell(EPW - 8, 5.5, quote, fill=True, new_x="LMARGIN", new_y="NEXT")
        yend = pdf.get_y()
        pdf.set_draw_color(*GOLD); pdf.set_line_width(0.8)
        pdf.line(18.5, ystart, 18.5, yend)
        pdf.set_text_color(*DARK)
    # options 2x2
    pdf.set_font("Noto", "", 10)
    pdf.ln(0.5)
    colw = EPW/2
    for i in range(0, 4, 2):
        pdf.set_x(18)
        pdf.cell(colw-3, 6, opts[i])
        pdf.cell(colw-3, 6, opts[i+1], new_x="LMARGIN", new_y="NEXT")
    pdf.ln(2.5)

title_block()

# ---- Questions data ----
section("PART 1.  7주차 — 심리적 문제의 진단과 분류")
question(1, "다음 중 이상행동의 정의 기준에 해당하지 않는 것은?",
         ["① 규범에 대한 동조","② 주관적 고통","③ 무능력 또는 역기능","④ 생물학적 유전성"])
question(2, "다음 중 '규범에 대한 동조(통계적 희귀성)' 기준의 단점으로 잘못된 것은?",
         ["① 절단점 선택의 문제","② 일탈의 종류의 문제","③ 문화적 상대성의 문제","④ 주관적 고통의 양 결정 문제"])
question(3, "다음 중 진단(분류 체계)이 가진 이점에 해당하지 않는 것은?",
         ["① 의사소통","② 경험적 연구의 촉진","③ 이상행동의 원인 연구","④ 치료 비용의 절감"])
question(4, "다음 중 DSM 체계에 대한 설명으로 옳지 않은 것은?",
         ["① DSM-III에서 다축체계 첫 등장","② DSM-5에서 다축체계 폐지","③ DSM-5는 WHODAS 2.0 사용","④ 가장 최신판은 DSM-IV-TR"])
question(5, "최초의 현대적 정신의학 진단 및 분류체계의 형식을 구현한 학자는?",
         ["① 에밀 크레펠린","② 벤저민 러시","③ 빌헬름 분트","④ 지그문트 프로이트"])
question(6, "다음 중 DSM-5-TR(2022)에서 새로 추가된 진단명에 해당하지 않는 것은?",
         ["① 지속성 애도 장애","② 비자살적 자해","③ 명시되지 않은 기분 장애","④ 신체이형장애"])
question(7, "아래의 설명에 해당하는 정신장애 정의의 출처를 고르시오.",
         ["① DSM-5의 정의","② ICD-8의 정의","③ 크레펠린의 정의","④ WHO 헌장"],
         quote='"정신기능의 기초를 이루는 심리적·생물학적·발달과정에서의 기능이상을 반영하는 개인의 인지, 정서조절, 또는 행동에서 임상적으로 유의한 장해가 특징인 증후군"')
question(8, "다음 중 '주관적 고통' 기준에 대한 설명으로 잘못된 것은?",
         ["① 주관적 감정에 초점을 둔다","② 스스로 문제를 평가할 수 있다","③ 고통을 보고 안 하는 경우는 없다","④ 고통의 양을 결정하기 쉽지 않다"])

section("PART 2.  9주차 — 임상 면담")
question(9, "다음 중 평가면담에 대한 설명으로 잘못된 것은?",
         ["① 가장 기본적·유용한 방법이다","② 일반 대화와 검사의 중간이다","③ 검사보다 더 공식적·표준화됨","④ 구조화 진단면담은 검사에 가깝다"])
question(10, "다음 중 라포(Rapport)에 대한 설명으로 옳지 않은 것은?",
         ["① 환자-임상가 관계의 질이다","② 면담의 필요충분조건이다","③ 기본은 존경·수용적 태도이다","④ 친구가 되는 것은 아니다"])
question(11, "다음 중 비밀유지의 예외조항에 해당하지 않는 것은?",
         ["① 자살 위험","② 타해 위험","③ 범죄 신고의 의무","④ 보호자의 단순 요청"])
question(12, "평가 면담 기법의 일반적 순서로 올바르게 나열된 것은?",
         ["① 경청-공감-명료화-질문-반영-요약","② 질문-경청-명료화-공감-요약-반영","③ 공감-경청-질문-반영-명료화-요약","④ 경청-질문-공감-명료화-반영-요약"])
question(13, "다음 중 마음챙김 면담에서 면담자에게 가장 빈번하게 문제가 되는 감정은?",
         ["① 분노","② 불안","③ 슬픔","④ 지루함"])
question(14, "아래의 설명이 가리키는 정신상태평가 항목을 고르시오.",
         ["① 착각","② 환각","③ 지남력","④ 현실검증력"],
         quote='"실제로는 외부에 자극이 없는데 자극이 있는 것처럼 지각하는 것"')
question(15, "다음 중 면담의 진행과정 3단계를 순서대로 바르게 나열한 것은?",
         ["① 초기-중기-종료","② 중기-초기-종료","③ 초기-종료-중기","④ 종료-중기-초기"])
question(16, "다음 중 조현병 환자와의 면담 방식으로 잘못된 것은?",
         ["① 망상·환청을 면전에서 반박한다","② 중립적·공감적으로 대한다","③ 다른 정보원을 면담한다","④ \"힘드셨겠습니다\"라고 반응한다"])
question(17, "다음 중 정신상태평가(MSE)의 내용에 해당하지 않는 것은?",
         ["① 지남력","② 현실검증력","③ 지각의 혼란","④ 가족의 경제 수준"])

section("PART 3.  10주차 — 임상적 개입")
question(18, "아래의 설명이 가리키는 개념의 명칭을 고르시오.",
         ["① 효과(effectiveness)","② 효능(efficacy)","③ 신뢰도","④ 일반화"],
         quote='"치료를 받은 사람이 받지 않은 사람보다 통계적으로 유의미하게 기능이 향상되었을 때 인정되며, 내적 타당도가 강조된다"')
question(19, "다음 중 효과(effectiveness) 연구의 특징으로 잘못된 것은?",
         ["① 실제 환경의 치료와 유사하다","② 내담자·상담자가 더 이질적이다","③ 외적 타당도가 강조된다","④ 무선할당된 잘 통제된 실험이다"])
question(20, "다음 중 근거기반실천(EBP)을 구성하는 세 요소에 해당하지 않는 것은?",
         ["① 과학적 연구결과","② 임상적 전문성","③ 환자의 필요와 선호","④ 건강보험 수가 기준"])
question(21, "다음 중 피험자 내 연구설계의 대표 유형에 해당하는 것은?",
         ["① 혼합설계","② 반전 설계","③ 집단 간 설계","④ 준 실험 설계"])
question(22, "다음 중 내적·외적 타당도에 대한 설명으로 옳지 않은 것은?",
         ["① 내적=독립변인 조작에 의함 증명","② 외적=일반화 가능 정도","③ 표본 한정하면 외적 타당도↑","④ 내적 추구는 실험실 한정 결과 우려"])
question(23, "다음 중 혼합설계에 대한 설명으로 잘못된 것은?",
         ["① 치료효과 연구에서 가장 널리 사용","② 집단 간+집단 내 설계 혼합","③ 무선선발·무선할당이 핵심","④ 단일 피험자로 과학적 인정 쉬움"])
question(24, "기존 문헌의 실증연구 결과(효과크기)를 수집·통계 처리하는 정량적 문헌연구 방법은?",
         ["① 사례연구","② 메타분석","③ 관찰연구","④ 델파이 기법"])
question(25, "다음 중 심리적 개입에 대한 Wolberg(1967)의 정의의 특징으로 옳은 것은?",
         ["① 통계적 색채가 짙다","② 의학적 색채가 짙다","③ 철학적 색채가 짙다","④ 종교적 색채가 짙다"])

section("PART 4.  11주차 — 정신재활")
question(26, "다음 중 정신재활의 3대 목표에 해당하지 않는 것은?",
         ["① 재기","② 지역사회통합","③ 삶의 질","④ 증상의 완전한 제거"])
question(27, "아래의 설명이 가리키는 정신재활의 목표를 고르시오.",
         ["① 재기(recovery)","② 지역사회통합","③ 삶의 질","④ 탈시설화"],
         quote='"질병의 한계에도 불구하고 삶에 만족하고 희망을 가지며, 정체성을 재확립하고 삶의 새로운 의미·목적을 개발하는 것"')
question(28, "WHO 분류 확장모델에서 '환각, 망상, 우울'이 해당하는 단계는?",
         ["① 손상(Impairment)","② 기능저하(Dysfunction)","③ 장애(Disability)","④ 불이익(Disadvantage)"])
question(29, "다음 중 정신재활의 핵심원리로 잘못된 것은?",
         ["① 기본 목적은 기능 향상이다","② 적극적 참여가 핵심이다","③ 의존성은 무조건 줄일 나쁜 것","④ 주거·교육·직업 성과에 초점"])
question(30, "다음 중 정신재활에 대한 오해(틀린 명제)에 해당하지 않는 것은?",
         ["① 만성화되면 재활 불가능","② 성과는 자격증에 따라 차이","③ 약물치료만으로 좋은 성과","④ 재활과 약물치료는 상호 보완적"])
question(31, "다음 중 사례관리의 성공 조건에 해당하지 않는 것은?",
         ["① 지속성","② 접근성","③ 책임성","④ 강제성"])
question(32, "아래의 설명이 가리키는 정신재활 프로그램의 명칭을 고르시오.",
         ["① 클럽하우스","② 적극적 지역사회 치료(PACT)","③ 낮병원","④ 지원고용"],
         quote='"다학문적 전문가 팀 접근으로 포괄적 서비스를 제공하며, 병원 입원치료의 대안으로 개발된 가장 면밀하게 연구된 근거중심 서비스"')
question(33, "다음 중 클럽하우스에 대한 설명으로 잘못된 것은?",
         ["① 내담자는 환자가 아닌 회원이다","② 일 중심 일과를 강조한다","③ 병원이 운영하는 부분입원이다","④ 직원과 책임·의무를 공유한다"])
question(34, "다음 중 직업재활에 대한 설명으로 옳지 않은 것은?",
         ["① 독립적 경제력 회복이 목적","② 지원고용=정규임금·정규직","③ 1999년 장애인복지법 개정 포함","④ 정신재활에서 가장 후순위 과제"])

section("PART 5.  13주차 — 임상심리학의 미래")
question(35, "다음 중 알파고와 이세돌 대국에 대한 설명으로 옳지 않은 것은?",
         ["① 2016년 3월 딥마인드 챌린지","② 알파고가 5국 중 4국 승리","③ 이세돌은 1승도 못 거뒀다","④ 인공지능 인식을 바꾼 사건"])
question(36, "아래의 설명이 가리키는 기술의 명칭을 고르시오.",
         ["① 가상현실(VR)","② 증강현실(AR)","③ 메타버스","④ 딥러닝"],
         quote='"현실세계에 가상 물체를 겹쳐 보여주는 기술로, 대표적으로 포켓몬Go가 있다"')
question(37, "최초의 자연어 습득 인공지능 'ELIZA'를 개발한 학자는?",
         ["① 와이젠바움(Weizenbaum)","② 앤서니(Anthony)","③ 크레펠린","④ 풀머(Fulmer)"])
question(38, "다음 중 대표적인 심리상담 챗봇(chatbot)에 해당하지 않는 것은?",
         ["① TESS","② Woebot","③ EndeavorRx","④ CBT 기반 챗봇"])
question(39, "미국 FDA가 승인한 최초의 게임 기반 치료로, ADHD 주의력 개선용 비디오 게임은?",
         ["① ELIZA","② EndeavorRx","③ 포켓몬Go","④ Woebot"])
question(40, "다음 중 미래의 임상심리학자가 갖추어야 할 자세로 잘못된 것은?",
         ["① 새 기술을 기존 이론·경험과 통합","② AI·메타버스 용어에 익숙","③ 기존 이론 버리고 기술에만 의존","④ 새 정신질환 대응 도구 보유"])

# ---- Answer page ----
pdf.add_page()
pdf.set_font("Noto", "B", 14)
pdf.set_text_color(*BLUE)
pdf.cell(0, 9, "✓ 정답 및 해설", new_x="LMARGIN", new_y="NEXT")
y = pdf.get_y(); pdf.set_draw_color(*BLUE); pdf.set_line_width(0.5); pdf.line(15, y, 15+EPW, y)
pdf.ln(3)

answers = [
 (1,"④","3기준=규범·고통·역기능"),(2,"④","④는 주관적 고통 단점"),
 (3,"④","이점4: 소통·연구·원인·근거"),(4,"④","최신=DSM-5-TR(2022)"),
 (5,"①","크레펠린"),(6,"④","신체이형장애=DSM-III-R"),
 (7,"①","DSM-5 정의"),(8,"③","고통 미보고 多"),
 (9,"③","검사보다 덜 표준화"),(10,"②","필요조건"),
 (11,"④","자살·타해·법적·범죄신고"),(12,"①","경공명질반요"),
 (13,"②","불안"),(14,"②","환각"),
 (15,"①","초기-중기-종료"),(16,"①","면전 반박 금지"),
 (17,"④","경제수준은 MSE 아님"),(18,"②","효능(efficacy)"),
 (19,"④","④는 효능 특징"),(20,"④","연구+전문성+선호"),
 (21,"②","반전·다중기저선"),(22,"③","한정시 내적↑외적↓"),
 (23,"④","큰표본·무선할당 필요"),(24,"②","메타분석"),
 (25,"②","의학적 색채"),(26,"④","재활은 증상과 공존"),
 (27,"①","재기"),(28,"①","손상"),
 (29,"③","의존성은 나쁜것 아님"),(30,"④","④는 옳은 명제"),
 (31,"④","지속·접근·책임·효율"),(32,"②","PACT"),
 (33,"③","부분입원=낮병원"),(34,"④","직업성과는 최우선"),
 (35,"③","이세돌 1승"),(36,"②","증강현실(AR)"),
 (37,"①","와이젠바움"),(38,"③","EndeavorRx=게임치료"),
 (39,"②","EndeavorRx"),(40,"③","기존과 통합해야"),
]

# two-column answer table
pdf.set_font("Noto","B",8.5)
col_n, col_a, col_e = 10, 12, (EPW/2 - 22)
def head():
    pdf.set_fill_color(*BLUE); pdf.set_text_color(255,255,255)
    for _ in range(2):
        pdf.cell(col_n,7,"#",border=1,align="C",fill=True)
        pdf.cell(col_a,7,"답",border=1,align="C",fill=True)
        pdf.cell(col_e,7,"해설",border=1,align="C",fill=True)
    pdf.ln()
head()
pdf.set_font("Noto","",8.5)
half = 20
for r in range(half):
    left = answers[r]; right = answers[r+half]
    pdf.set_text_color(*DARK)
    pdf.cell(col_n,6.5,str(left[0]),border=1,align="C")
    pdf.set_text_color(*RED); pdf.cell(col_a,6.5,left[1],border=1,align="C")
    pdf.set_text_color(*DARK); pdf.cell(col_e,6.5,left[2],border=1)
    pdf.cell(col_n,6.5,str(right[0]),border=1,align="C")
    pdf.set_text_color(*RED); pdf.cell(col_a,6.5,right[1],border=1,align="C")
    pdf.set_text_color(*DARK); pdf.cell(col_e,6.5,right[2],border=1)
    pdf.ln()

pdf.ln(4)
pdf.set_fill_color(*TIPBG); pdf.set_draw_color(*GOLD); pdf.set_line_width(0.3)
pdf.set_text_color(*DARK); pdf.set_font("Noto","",9)
tip = ("[ 출제 경향 팁 ]  기출은 부정형(\"아닌/잘못된/옳지 않은\") 문제가 약 65%로 가장 많습니다. "
       "문제 끝부분을 끝까지 읽는 습관과, 4개 보기 중 1개의 틀린 보기를 찾는 연습이 핵심입니다. "
       "정의→용어 매칭, 순서 나열, 잘못 연결된 것 찾기 유형도 반드시 대비하세요!")
pdf.multi_cell(EPW, 5.5, tip, border=1, fill=True)

pdf.output("/home/user/web1/임상심리학_시험공부/임상심리학_모의고사.pdf")
print("PDF 생성 완료")
