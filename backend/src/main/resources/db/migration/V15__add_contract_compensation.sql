ALTER TABLE employment_contracts ADD COLUMN base_salary_amount NUMERIC(17, 0);
ALTER TABLE employment_contracts ADD COLUMN monthly_bonus_amount NUMERIC(17, 0);
ALTER TABLE employment_contracts ADD COLUMN bonus_details VARCHAR(2000);
ALTER TABLE employment_contracts ADD COLUMN other_benefits VARCHAR(2000);
