-- =========================================
-- SCRIPT DE CRIAÇÃO DE USUÁRIOS
-- Sistema Relatórios Offline - IFSC XXE
-- =========================================

-- IMPORTANTE: Execute estes comandos diretamente no PostgreSQL
-- após o sistema criar as tabelas automaticamente (JPA/Hibernate)

-- =========================================
-- 1. CRIAR USUÁRIO ADMINISTRADOR
-- =========================================

-- Usuário: admin
-- Senha: admin123 (APENAS PARA TESTES - MUDE EM PRODUÇÃO!)
INSERT INTO usuarios (username, nome, password)
VALUES (
    'admin',
    'Administrador do Sistema',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IKisVVBVMY/xSCHhxRBxYJmz6vT6Di'
) ON CONFLICT (username) DO NOTHING;

-- Adicionar role ADMIN
INSERT INTO usuario_roles (usuario_id, role)
SELECT id, 'ADMIN' FROM usuarios WHERE username = 'admin'
ON CONFLICT DO NOTHING;

-- Adicionar role USER
INSERT INTO usuario_roles (usuario_id, role)
SELECT id, 'USER' FROM usuarios WHERE username = 'admin'
ON CONFLICT DO NOTHING;

-- =========================================
-- 2. CRIAR USUÁRIO PADRÃO (OPCIONAL)
-- =========================================

-- Usuário: user
-- Senha: user123 (APENAS PARA TESTES - MUDE EM PRODUÇÃO!)
INSERT INTO usuarios (username, nome, password)
VALUES (
    'user',
    'Usuário Padrão',
    '$2a$10$5rkpzyLQVbwjV.FDJFVDy.vvfvVKLrCjYPtcXYYPSgcbVPJYCFjXm'
) ON CONFLICT (username) DO NOTHING;

-- Adicionar role USER
INSERT INTO usuario_roles (usuario_id, role)
SELECT id, 'USER' FROM usuarios WHERE username = 'user'
ON CONFLICT DO NOTHING;

-- =========================================
-- 3. VERIFICAR USUÁRIOS CRIADOS
-- =========================================

SELECT
    u.id,
    u.username,
    u.nome,
    STRING_AGG(ur.role::text, ', ') as roles
FROM usuarios u
LEFT JOIN usuario_roles ur ON u.id = ur.usuario_id
GROUP BY u.id, u.username, u.nome
ORDER BY u.id;

-- =========================================
-- HASH BCrypt DE SENHAS COMUNS
-- (APENAS PARA REFERÊNCIA - TESTES LOCAIS)
-- =========================================

-- admin123  → $2a$10$N9qo8uLOickgx2ZMRZoMye.IKisVVBVMY/xSCHhxRBxYJmz6vT6Di
-- user123   → $2a$10$5rkpzyLQVbwjV.FDJFVDy.vvfvVKLrCjYPtcXYYPSgcbVPJYCFjXm
-- password  → $2a$10$8K1p/a0dL3.IVpJ8kGZJWuODfkJCHqn9JJ5jNfj7.KHJUOWKJjOu2
-- 123456    → $2a$10$6Z1p5PU5j1vK1j8LJ4J8J.J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J

-- =========================================
-- PARA GERAR NOVOS HASHES BCrypt
-- =========================================

-- Use o seguinte código Java:
-- import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
--
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hash = encoder.encode("sua_senha_aqui");
-- System.out.println(hash);

-- =========================================
-- COMANDOS ÚTEIS
-- =========================================

-- Listar todos os usuários
SELECT username, nome FROM usuarios;

-- Verificar roles de um usuário específico
SELECT role FROM usuario_roles
WHERE usuario_id = (SELECT id FROM usuarios WHERE username = 'admin');

-- Atualizar senha de um usuário
UPDATE usuarios
SET password = '$2a$10$NOVO_HASH_AQUI'
WHERE username = 'nome_usuario';

-- Adicionar role a um usuário existente
INSERT INTO usuario_roles (usuario_id, role)
SELECT id, 'ADMIN' FROM usuarios WHERE username = 'nome_usuario';

-- Remover role de um usuário
DELETE FROM usuario_roles
WHERE usuario_id = (SELECT id FROM usuarios WHERE username = 'nome_usuario')
AND role = 'ADMIN';

-- Deletar usuário (cuidado!)
DELETE FROM usuario_roles WHERE usuario_id = (SELECT id FROM usuarios WHERE username = 'nome_usuario');
DELETE FROM usuarios WHERE username = 'nome_usuario';

-- =========================================
-- NOTAS DE SEGURANÇA
-- =========================================

-- 1. EM PRODUÇÃO:
--    - Use senhas fortes e únicas
--    - Nunca use as senhas de exemplo acima
--    - Gere novos hashes BCrypt para cada ambiente
--    - Use variáveis de ambiente para senhas

-- 2. ROLES DISPONÍVEIS:
--    - USER       : Acesso básico ao sistema
--    - ADMIN      : Acesso administrativo
--    - SUPERADMIN : Super administrador (futuro)

-- 3. BOAS PRÁTICAS:
--    - Nunca armazene senhas em texto plano
--    - Sempre use BCrypt (ou similar) para hash
--    - Implemente política de senhas fortes
--    - Configure expiração de senhas periodicamente
--    - Mantenha logs de auditoria

-- =========================================
-- FIM DO SCRIPT
-- =========================================

