-- Test data
INSERT INTO role (role_name, role_description) VALUES ('Developer', 'Software Developer');
INSERT INTO role (role_name, role_description) VALUES ('Project Manager', 'Project Manager');

INSERT INTO employee (username, password, email, role) VALUES ('admin', 'admin123', 'admin@test.com', 'Project Manager');
INSERT INTO employee (username, password, email, role) VALUES ('dev1', 'dev123', 'dev1@test.com', 'Team Member');

INSERT INTO employee_role (employee_id, role_id) VALUES (1, 2);
INSERT INTO employee_role (employee_id, role_id) VALUES (2, 1);
