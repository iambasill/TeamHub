INSERT INTO departments (id, name, description, created_at, updated_at)
SELECT gen_random_uuid(), dept.name, dept.description, NOW(), NOW()
FROM (VALUES
    ('Engineering',                    'Software engineering and development teams'),
    ('Product Management',             'Product strategy, roadmap, and delivery'),
    ('Design (UI/UX)',                 'User interface and user experience design'),
    ('Quality Assurance / Testing',    'QA processes, test planning, and automation'),
    ('DevOps / Infrastructure',        'Infrastructure, CI/CD, and platform reliability'),
    ('Data & Analytics',               'Data engineering, analytics, and insights'),
    ('Sales',                          'Sales strategy, pipeline, and revenue growth'),
    ('Marketing',                      'Brand, growth, content, and demand generation'),
    ('Customer Success / Support',     'Customer onboarding, retention, and support'),
    ('Human Resources',                'People operations, hiring, and culture'),
    ('Finance & Accounts',             'Financial planning, accounting, and reporting'),
    ('Legal & Compliance',             'Legal counsel, contracts, and regulatory compliance'),
    ('Operations',                     'Business operations and process efficiency'),
    ('IT & Security',                  'IT infrastructure, tooling, and information security'),
    ('Business Development / Partnerships', 'Strategic partnerships and new business opportunities'),
    ('Executive / Leadership',         'C-suite and senior leadership team')
) AS dept(name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM departments d WHERE d.name = dept.name
);

INSERT INTO roles (id, name, description)
SELECT gen_random_uuid(), r.name, r.description
FROM (VALUES
    ('Software Engineer',                    'Develops and maintains software systems'),
    ('Senior Software Engineer',             'Leads technical implementation and mentors juniors'),
    ('Engineering Manager',                  'Manages engineering teams and delivery'),
    ('Tech Lead',                            'Technical leadership and architecture decisions'),
    ('Product Manager',                      'Owns product vision, roadmap, and execution'),
    ('Product Designer / UI/UX Designer',    'Designs user interfaces and experiences'),
    ('QA Engineer',                          'Ensures product quality through testing'),
    ('DevOps Engineer',                      'Manages CI/CD pipelines and infrastructure automation'),
    ('Site Reliability Engineer (SRE)',      'Maintains service reliability and uptime'),
    ('Data Analyst',                         'Analyses data to generate business insights'),
    ('Data Scientist',                       'Builds models and extracts insights from complex data'),
    ('Data Engineer',                        'Builds and maintains data pipelines and warehouses'),
    ('Sales Executive',                      'Drives new sales and manages client relationships'),
    ('Sales Manager',                        'Leads sales teams and sets targets'),
    ('Marketing Executive',                  'Executes marketing campaigns and brand activities'),
    ('Marketing Manager',                    'Plans and oversees marketing strategy'),
    ('Content Strategist',                   'Develops content plans and editorial direction'),
    ('Customer Support Specialist',          'Handles customer queries and issue resolution'),
    ('Customer Success Manager',             'Ensures customer satisfaction and long-term retention'),
    ('HR Manager',                           'Leads HR operations, policies, and people strategy'),
    ('Recruiter',                            'Sources, screens, and onboards new hires'),
    ('Accountant / Finance Analyst',         'Manages financial records and analysis'),
    ('Legal Counsel',                        'Provides legal advice and manages compliance'),
    ('Operations Manager',                   'Oversees day-to-day operational processes'),
    ('IT Administrator',                     'Manages internal IT systems and user support'),
    ('Security Engineer',                    'Implements and monitors information security controls'),
    ('Business Development Manager',         'Identifies and develops strategic business opportunities'),
    ('CEO / Founder',                        'Chief Executive Officer — overall company leadership'),
    ('CTO',                                  'Chief Technology Officer — technology vision and strategy'),
    ('COO',                                  'Chief Operating Officer — operational excellence'),
    ('Intern',                               'Entry-level trainee across any department')
) AS r(name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM roles ro WHERE ro.name = r.name
);
