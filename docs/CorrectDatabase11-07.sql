ALTER TABLE DEGREE_CURRICULAR_PLAN DROP KEY_DEGREE_CURRICULAR_PLAN_ENROLMENT_INFO;
ALTER TABLE DEGREE_CURRICULAR_PLAN CHANGE DEGREE_DURATION DEGREE_DURATION INT(11)  DEFAULT "5" NOT NULL;
ALTER TABLE DEGREE_CURRICULAR_PLAN CHANGE MINIMAL_YEAR_FOR_OPTIONAL_COURSES MINIMAL_YEAR_FOR_OPTIONAL_COURSES INT(11)  DEFAULT "3" NOT NULL;

UPDATE DEGREE_CURRICULAR_PLAN set MARK_TYPE = 5 where NAME like 'M%';

UPDATE DEGREE_CURRICULAR_PLAN SET MARK_TYPE= 20 WHERE ID_INTERNAL=24;
UPDATE DEGREE_CURRICULAR_PLAN SET MARK_TYPE= 20 WHERE ID_INTERNAL=29;
UPDATE DEGREE_CURRICULAR_PLAN SET MARK_TYPE= 20 WHERE ID_INTERNAL=33;
UPDATE DEGREE_CURRICULAR_PLAN SET MARK_TYPE= 20 WHERE ID_INTERNAL=34;

UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 26 WHERE ID_INTERNAL=26;
UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 30 WHERE ID_INTERNAL=31;
UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 23.5 WHERE ID_INTERNAL=38;
UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 16 WHERE ID_INTERNAL=39;
UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 22.5 WHERE ID_INTERNAL=41;
UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 21 WHERE ID_INTERNAL=42;
UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 27 WHERE ID_INTERNAL=49;


INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (4, 2, 1, 'Conclus�o de Mestrado', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (5, 2, 1, 'Conclus�o de Doutoramento', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (6, 2, 1, 'Conclus�o Agrega��o', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (7, 2, 1, 'Conclus�o da Parte Escolar do Mestrado', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (8, 2, 1, 'Conclus�o de Aptid�o Pedag�gica e Capacidade Cient�fica', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (9, 2, 1, 'Conclus�o de Cursos de Especializa��o n�o Conferentes de Grau', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (10, 2, 1, 'Conclus�o de Equival�ncia ao Grau de Licenciado', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (11, 2, 1, 'Conclus�o de Equival�ncia ao Grau de Mestre', 15);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (12, 2, 1, 'De Inscri��o, Frequ�ncia ou Exame de uma Disciplina, Trabalho ou Est�gio', 7);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (13, 2, 1, 'De Inscri��o, Frequ�ncia ou Exame por Disciplina, Trabalho ou est�gio a mais', 1);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (14, 2, 1, 'Matr�cula', 7);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (15, 2, 1, 'Programas (Pela Primeira Folha)', 5);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (16, 2, 1, 'Programas (Por Cada que Exceda a Primeira)', 0.5);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (17, 2, 1, 'N�o Especificada', 7);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (18, 2, 1, 'Por Fotoc�pia (Pela Primeira Folha)', 5);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (19, 2, 1, 'Por Fotoc�pia (Por Cada que Exceda a Primeira)', 0.5);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (20, 2, 2, 'Agrega��o', 200);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (21, 2, 2, 'Doutoramento', 150);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (22, 2, 2, 'Mestrado', 125);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (23, 2, 2, 'Parte Escolar do Mestrado', 75);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (24, 2, 2, 'Curso de Especializa��o', 75);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (25, 2, 2, 'Outros Diplomas', 75);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (26, 2, 3, 'Agrega��o', 600);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (27, 2, 3, 'Doutoramento', 550);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (28, 2, 3, 'Mestrado', 150);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (29, 2, 3, 'Aptid�o Pedag�gica e Capacidade Cient�fica', 150);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (30, 2, 8, 'Mestrado', 500);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (31, 2, 8, 'Licenciatura', 400);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (32, 2, 8, 'Exames ad hoc Previstos no Decreto-Lei n�283/83 de 21 de Junho', 100);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (33, 2, 8, 'Equival�ncia por Disciplina', 25);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (34, 2, 5, 'Mestrado', 10);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (35, 2, 5, 'Doutoramento', 10);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (36, 2, 5, 'Curso de Especializa��o', 10);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (37, 2, 5, 'Substitui��o de Disciplinas (Anula��o / Inscri��o por Cada Disciplina)', 10);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (38, 2, 6, 'Inscri��o Fora de Prazo', 50);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (39, 2, 7, 'Mestrados e Doutoramentos', 5);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (40, 2, 7, 'Curso de Especializa��o', 5);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (41, 2, 10, 'Pagamento na Totalidade', 2000);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (42, 2, 10, 'Pagamento Parcial de 50%', 1000);
INSERT INTO PRICE (ID_INTERNAL, GRADUATION_TYPE, DOCUMENT_TYPE, DESCRIPTION, PRICE) VALUES (43, 2, 10, 'Pagamento Parcial de 25%', 500);

ALTER TABLE CURRICULAR_COURSE DROP UNIVERSITY_CODE;
ALTER TABLE CURRICULAR_COURSE ADD KEY_UNIVERSITY INT(11);
ALTER TABLE CURRICULAR_COURSE_SCOPE ADD CREDITS DOUBLE;

ALTER TABLE MASTER_DEGREE_CANDIDATE ADD SUBSTITUTE_ORDER INT(11)  DEFAULT null;
ALTER TABLE DEGREE_CURRICULAR_PLAN ADD NUMERUS_CLAUSUS INT(11)  UNSIGNED DEFAULT null;

UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 150 WHERE ID_INTERNAL=24;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 40 WHERE ID_INTERNAL=25;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 30 WHERE ID_INTERNAL=26;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 30 WHERE ID_INTERNAL=27;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 35 WHERE ID_INTERNAL=28;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 30 WHERE ID_INTERNAL=29;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 30 WHERE ID_INTERNAL=30;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 30 WHERE ID_INTERNAL=31;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 25 WHERE ID_INTERNAL=32;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 40 WHERE ID_INTERNAL=33;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 25 WHERE ID_INTERNAL=34;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 40 WHERE ID_INTERNAL=35;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 15 WHERE ID_INTERNAL=36;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 20 WHERE ID_INTERNAL=37;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 16 WHERE ID_INTERNAL=38;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 16 WHERE ID_INTERNAL=38;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 25 WHERE ID_INTERNAL=40;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 60 WHERE ID_INTERNAL=43;
UPDATE DEGREE_CURRICULAR_PLAN SET NUMERUS_CLAUSUS= 25 WHERE ID_INTERNAL=48;

UPDATE DEGREE_CURRICULAR_PLAN SET NEEDED_CREDITS= 24 WHERE ID_INTERNAL=112;



drop table if exists QUALIFICATION;
create table QUALIFICATION(
   ID_INTERNAL integer(11) not null auto_increment,
   KEY_PERSON integer(11) not null ,
   YEAR integer(11),
   MARK varchar(200),
   SCHOOL varchar(200),
   TITLE varchar(200),
   primary key (ID_INTERNAL))
   type=InnoDB;

   
drop table if exists CREDITS_MANAGER_DEPARTMENT;
create table CREDITS_MANAGER_DEPARTMENT (
   ID_INTERNAL int(11) not null auto_increment,
   KEY_PERSON int(11) not null,
   KEY_DEPARTMENT int(11) not null,
   primary key (ID_INTERNAL),
   unique U1 (KEY_PERSON, KEY_DEPARTMENT))
   type=InnoDB;   
   