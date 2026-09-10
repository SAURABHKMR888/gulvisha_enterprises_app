import psycopg2

conn = psycopg2.connect('dbname=gulvisha user=postgres password=postgress host=localhost')
cur = conn.cursor()
cur.execute('UPDATE services SET display_order = 0 WHERE display_order IS NULL')
conn.commit()
print('Updated', cur.rowcount, 'rows')
cur.execute('UPDATE services SET display_order = 0 WHERE display_order IS NULL')
conn.commit()
print('Verified:', cur.rowcount, 'nulls remaining')
conn.close()
