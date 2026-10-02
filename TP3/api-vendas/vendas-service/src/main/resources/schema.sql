CREATE TABLE IF NOT EXISTS venda (
    id_venda      BIGSERIAL     PRIMARY KEY,
    id_produto    BIGINT        NOT NULL,
    quantidade    INTEGER       NOT NULL CHECK (quantidade > 0),
    valor_produto NUMERIC(12,2) NOT NULL,
    valor_total   NUMERIC(12,2) NOT NULL,
    usuario       VARCHAR(50)   NOT NULL,
    data_venda    TIMESTAMP     NOT NULL
);
