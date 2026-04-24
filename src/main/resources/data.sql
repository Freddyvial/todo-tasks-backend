DELETE FROM task_items;
DELETE FROM tasks;

INSERT INTO tasks (title, description, execution_date, status, created_at, updated_at) VALUES
-- 1 - 6 (las tuyas)
('Preparar demo para cliente', 'Tarea programada a futuro para validar listados base', DATEADD('DAY', 2, CURRENT_TIMESTAMP), 'PROGRAMMED', DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
('Enviar informe semanal', 'Tarea programada cuya fecha ya llego para validar dueNowAlert', DATEADD('HOUR', -6, CURRENT_TIMESTAMP), 'PROGRAMMED', DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -8, CURRENT_TIMESTAMP)),
('Refactor modulo de tareas', 'Tarea en progreso con varios items y avances parciales', DATEADD('HOUR', 4, CURRENT_TIMESTAMP), 'IN_PROGRESS', DATEADD('DAY', -5, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
('Cerrar sprint actual', 'Tarea finalizada con todos los items completos', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'FINISHED', DATEADD('DAY', -10, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
('Reunion cancelada con proveedor', 'Tarea cancelada para probar filtros por estado', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'CANCELLED', DATEADD('DAY', -4, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
('Depurar error en produccion', 'Tarea programada vencida sin items para revisar alertas y metricas en cero', DATEADD('DAY', -3, CURRENT_TIMESTAMP), 'PROGRAMMED', DATEADD('DAY', -6, CURRENT_TIMESTAMP), DATEADD('DAY', -2, CURRENT_TIMESTAMP)),

-- 7 - 25 (nuevas)
('Implementar login', 'Autenticacion de usuarios', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'IN_PROGRESS', DATEADD('DAY', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
('Configurar base de datos', 'Instalacion y configuracion inicial', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'FINISHED', DATEADD('DAY', -5, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
('Diseñar UI dashboard', 'Prototipo en Figma', DATEADD('DAY', 3, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Optimizar consultas SQL', 'Mejorar rendimiento', DATEADD('DAY', -2, CURRENT_TIMESTAMP), 'IN_PROGRESS', DATEADD('DAY', -4, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
('Actualizar dependencias', 'Actualizar librerias del proyecto', DATEADD('DAY', 2, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Crear endpoints API', 'Servicios REST iniciales', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'IN_PROGRESS', DATEADD('DAY', -3, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
('Pruebas unitarias', 'Cobertura de servicios', DATEADD('DAY', 4, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Revisar logs', 'Analisis de errores', DATEADD('HOUR', -2, CURRENT_TIMESTAMP), 'IN_PROGRESS', DATEADD('DAY', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
('Desplegar en staging', 'Preparar entorno QA', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Validar seguridad', 'Pruebas OWASP', DATEADD('DAY', 5, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Documentar API', 'Swagger docs', DATEADD('DAY', 2, CURRENT_TIMESTAMP), 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Implementar notificaciones', 'Emails y alertas', DATEADD('DAY', 3, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Migrar base de datos', 'Versionado schema', DATEADD('DAY', -3, CURRENT_TIMESTAMP), 'FINISHED', DATEADD('DAY', -7, CURRENT_TIMESTAMP), DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
('Analizar rendimiento frontend', 'Lighthouse metrics', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Configurar CI/CD', 'Pipeline automatizado', DATEADD('DAY', 2, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Soporte a usuarios', 'Resolver tickets', DATEADD('HOUR', -5, CURRENT_TIMESTAMP), 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Backup base de datos', 'Respaldo automatico', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Monitoreo sistema', 'Alertas y métricas', DATEADD('DAY', 3, CURRENT_TIMESTAMP), 'PROGRAMMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Revisión de código', 'Pull requests pendientes', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- TASK ITEMS (asumiendo IDs 1..25)
INSERT INTO task_items (description, completed, position, task_id) VALUES
-- ejemplos variados
('Definir guion de presentacion', FALSE, 1, 1),
('Validar ambiente de pruebas', FALSE, 2, 1),

('Revisar metricas del informe', TRUE, 1, 2),
('Adjuntar conclusiones finales', FALSE, 2, 2),

('Analizar codigo legado', TRUE, 1, 3),
('Aplicar cambios en servicio', TRUE, 2, 3),
('Validar pruebas automatizadas', FALSE, 3, 3),

('Confirmar historias completadas', TRUE, 1, 4),
('Actualizar tablero de seguimiento', TRUE, 2, 4),

('Notificar cancelacion al equipo', TRUE, 1, 5),

-- más items distribuidos
('Crear modelo de usuario', TRUE, 1, 7),
('Implementar JWT', FALSE, 2, 7),

('Instalar motor DB', TRUE, 1, 8),
('Crear esquema inicial', TRUE, 2, 8),

('Diseñar wireframes', FALSE, 1, 9),

('Indexar tablas', TRUE, 1, 10),

('Crear controlador REST', TRUE, 1, 12),
('Agregar validaciones', FALSE, 2, 12),

('Escribir pruebas unitarias', FALSE, 1, 13),

('Revisar errores recientes', TRUE, 1, 14),

('Configurar servidor QA', FALSE, 1, 15),

('Definir reglas seguridad', FALSE, 1, 16),

('Generar documentación', FALSE, 1, 17),

('Configurar correos', FALSE, 1, 18),

('Ejecutar migración', TRUE, 1, 19),

('Analizar tiempos carga', FALSE, 1, 20),

('Crear pipeline', FALSE, 1, 21),

('Responder tickets', FALSE, 1, 22),

('Programar backup', FALSE, 1, 23),

('Configurar alertas', FALSE, 1, 24),

('Revisar PRs', FALSE, 1, 25);