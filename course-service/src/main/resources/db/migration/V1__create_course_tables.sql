CREATE TABLE courses (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     instructor_id UUID NOT NULL,
     title VARCHAR(200) NOT NULL,
     description TEXT,
     level VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
     category VARCHAR(100),
     status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
     created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
     updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE modules (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
     title VARCHAR(200) NOT NULL,
     position INT NOT NULL
);

CREATE TABLE lessons (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
     title VARCHAR(200) NOT NULL,
     content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
     content_url_or_body TEXT,
     position INT NOT NULL,
     is_preview BOOLEAN NOT NULL DEFAULT false
);
