import pg from 'pg'
const { Client } = pg

const client = new Client({ database: 'gulvisha', user: 'postgres', password: 'postgress', host: 'localhost' })
await client.connect()
const res = await client.query('UPDATE services SET display_order = 0 WHERE display_order IS NULL')
console.log('Updated:', res.rowCount)
await client.end()
