CREATE TABLE IF NOT EXISTS public.test1 (
    id    BIGINT PRIMARY KEY,
    name  TEXT,
    score DOUBLE PRECISION
);

INSERT INTO public.test1 (id, name, score)
VALUES (1, 'alpha', 91.5),
       (2, 'beta', 87.0),
       (3, 'gamma', 76.5)
ON CONFLICT (id) DO NOTHING;

GRANT USAGE ON SCHEMA public TO benchmark;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.test1 TO benchmark;
