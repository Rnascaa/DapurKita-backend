INSERT INTO categories (id, name, slug) VALUES
    (1, 'Makanan Utama', 'makanan-utama'),
    (2, 'Lauk', 'lauk'),
    (3, 'Sayur', 'sayur'),
    (4, 'Sup dan Soto', 'sup-dan-soto'),
    (5, 'Sambal', 'sambal'),
    (6, 'Kue Tradisional', 'kue-tradisional'),
    (7, 'Jajanan', 'jajanan'),
    (8, 'Minuman', 'minuman');

SELECT setval(pg_get_serial_sequence('categories', 'id'), 8);
