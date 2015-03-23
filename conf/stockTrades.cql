DROP KEYSPACE IF EXISTS stocks;
CREATE KEYSPACE stocks WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
use stocks;

CREATE TABLE stock_ticks_price (
  tradeId uuid,
  symbol text,
  dateOffset int,
  tradeDate timestamp,
  priceAvg int,
  price decimal,
  quantity int,
  PRIMARY KEY ((symbol, dateOffset), priceAvg, tradeDate)
) WITH CLUSTERING ORDER BY (priceAvg DESC, tradeDate ASC)
   and gc_grace_seconds = 0;



CREATE TABLE stock_ticks_date (
  tradeId uuid,
  symbol text,
  dateOffset int,
  tradeDate timestamp,
  price decimal,
  quantity int,
  PRIMARY KEY ((symbol, dateOffset), tradeDate)
) WITH CLUSTERING ORDER BY (tradeDate DESC)
    and gc_grace_seconds = 0;


truncate stock_ticks_date;
truncate stock_ticks_price;

select distinct symbol, dateoffset from stock_ticks_date;

select * from stock_ticks_price where symbol='GOOG' and dateOffset=16514;

select * from stock_ticks_price where symbol='GOOG' and dateOffset=16514 order by priceAvg;

select * from stock_ticks_price where symbol='GOOG' and dateOffset=16514;

select * from stock_ticks_date where symbol='GOOG' and dateoffset=16521
    and tradeDate > '2015-03-19 14:47:09-0600' and tradeDate < '2015-03-19 14:47:22-0600' order by tradeDate;

select * from stock_ticks_price where symbol='GOOG' and dateoffset=16520 order by price;


CREATE TABLE stock_summary (
    symbol text,
    dateOffset int,
    open decimal,
    close decimal,
    low decimal,
    high decimal,
    totQuantity int,
    PRIMARY KEY (symbol, dateOffset)
) WITH CLUSTERING ORDER BY (dateOffset ASC)
    and gc_grace_seconds = 0;