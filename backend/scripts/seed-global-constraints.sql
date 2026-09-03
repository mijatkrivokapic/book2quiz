-- Seed the global_constraints table with the base quiz constraints
-- Run against the running Postgres container:
--   docker compose exec -T db psql -U <user> -d <db> < backend/scripts/seed-global-constraints.sql

INSERT INTO global_constraints (content) VALUES ('Sortiraj pitanja po težini.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Izbegavaj reprodukciju činjenica i fokusiraj se na analizu primera.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Definiši 1 hint za teža pitanja.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Ako pitanje sadrži kod, kod ne sme biti duži od 50 linija.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Sva pitanja treba da budu na srpskom jeziku.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Sav kod treba da bude na engleskom jeziku.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Tekst pitanja treba da bude u markdown formatu.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Hint ne treba da sadrži odgovor na pitanje, nego smernicu koja će pomoći učeniku da dođe do odgovora.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Tekst pitanja ne treba učenika da navodi na tačan odgovor, već da postavi problem i traži rešenje. Ako je učeniku potrebno usmerenje ka konceptu iz gradiva koje treba da primeni ono se navodi u hintu.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Ponuđeni odgovori ne treba da sadrže rezonovanje iza datog odgovora. Objašnjenja treba da budu u feedback polju.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Ako pitanje sadrži kod, kod treba biti u C# jeziku.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Pitanja koja od učenika traže primenu koncepata iz gradiva ne treba da budu plitke modifikacije primera navedenih u gradivu, već potpuno nove situacije.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Ako pitanje sadrži kod, pre navođenja koda dati opis uloge datog koda u sistemu ako je relevantan za razumevanje zadatka. Ako opis uloge koda nije relevantan za razumevanje zadatka nemoj ga navoditi.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('U feedback-u navedi samo ono što je relevantno za razumevanje razloga tačnosti datog odgovora, feedback ne treba da bude preopširan.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Kada pitanje proverava znanje heuristike, tekst pitanja ne sme imenovati heuristiku niti transformaciju koju treba primeniti. Učenik iz prikazanog koda sam prepoznaje uslove primene i imenuje transformaciju.') ON CONFLICT (content) DO NOTHING;
INSERT INTO global_constraints (content) VALUES ('Pitanja ne treba učenicima da se obraćaju u muškom ili ženskom rodu nego da budu rodno nezavisna.') ON CONFLICT (content) DO NOTHING;
