ALTER TABLE employees ADD COLUMN user_id UUID;
ALTER TABLE employees ADD CONSTRAINT fk_employees_user
    FOREIGN KEY (user_id) REFERENCES users(id);