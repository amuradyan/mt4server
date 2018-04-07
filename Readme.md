# MT4Server API

MT4Server is a REST API proxy between brokers who support MetaTrader4 platform. It provides such operations as listing of orders, buy and sell, balanse and ways to obtain ticker data via HTTP or web sockets(obtains a particular ticker from a broker).

It uses API key for authentication and JWT.

## Supported platforms
[AVA Trade](https://www.avatrade.com/)  
[ICMarkets](https://www.icmarkets.com/)  
[FXOpen](https://www.fxopen.com/)  
[ForexClub](https://www.fxclub.org/)  
[Just2Trade](https://just2trade.com/)  

## REST API

In the cURLs below youll see placeholders like \<this\>. These are describes below

\<mt4server\> - is at **52.164.255.82**  
\<API key\> - is **280EC427-5184-4AB4-8A12-EF4140057437**  
\<broker id\> - is the name by which MT4Server recongnises the broker. Currently the names are [fxclub, fxopen, ava, icmarkets, demo]  
\<JWT access token\> - is the access token you receive as a response from [Auth](#auth) endpoint  
\<order_id\> - is an ID of an existing order. All orders can be listed by calling the [Fetching all orders](#fetching-all-orders) endpoint

**Note**: demo is the name for a demo account with [Just2Trade](https://just2trade.com/) broker

#### Auth

Use this to obtain the JWT token

```http
curl -X POST \
  https://<mt4server>/token \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: text/plain' \
  -d <API key>
```
The response would be a JWT token which should be included in all the requests to endpoints below as an 'Authorization' header

#### Fetching all orders 

```http
curl -X GET \
  https://<mt4server>/exchanges/<broker id>/orders \
  -H 'Authorization: <JWT access token> \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
```

#### Creating buy and sell orders

* Buy order

```http
curl -X POST \
  https://<mt4server>/exchanges/<broker id>/orders \
  -H 'Authorization: <JWT access token> \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{
	"pair": "BTCUSD",
	"amount": 0.11,
	"op": "buy"
}'
```

* Sell order

```http
curl -X POST \
  https://<mt4server>/exchanges/<broker id>/orders \
  -H 'Authorization: <JWT access token> \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{
	"pair": "BTCUSD",
	"amount": 0.11,
	"op": "sell"
}'
```

#### Closing an order

```http
curl -X DELETE \
  https://<mt4server>/exchanges/<broker id>/orders/<order_id> \
  -H 'Authorization: <JWT access token> \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
```

#### Balance

```http
curl -X GET \
  https://<mt4server>/exchanges/<broker id>/balance \
  -H 'Authorization: <JWT access token> \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
```

#### Tickers Data

```http
curl -X GET \
  'https://<mt4server>/exchanges/<broker id>/tickers?tickers=BTCUSD,ETHUSD' \
  -H 'Authorization: <JWT access token> \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
```

#### WS ticker data

```http
    wss://<mt4server>/exchanges/<broker id>/ticker?ticker=BTCUSD
```

#### Ping

```http
curl -X GET \
  https://<mt4server>/ 
  -H 'Content-Type: application/json' \
```

## Broker tickers

Below are the brokers with their supported tickers:

- **AVA Trade** - [ EURUSD, USDJPY, BCHUSD, BTCEUR, BTCJPY, BTCUSD, BTGUSD ]  

- **ForexClub** - [ BTCEUR*, EURUSD, USDJPY, BTCUSD, ETCUSD, LTCBTC, LTCUSD, 
NEOUSD, ETHUSD, DSHUSD, XRPUSD, BCHUSD, BTGUSD, XMRUSD, ZECUSD ]  

- **FXOpen** - [ EURUSD*, USDJPY*, DSHBTC, DSHUSD, BCHUSD, BTCEUR, BTCUSD, BTCJPY, LTCBTC, LTCEUR, LTCJPY, LTCUSD, ETHEUR, ETHJPY, ETHUSD, XRPEUR, XRPUSD ]

- **ICMarkets** - [ EURUSD, USDJPY, BCHUSD, BTCUSD, DSHUSD, ETHUSD, LTCUSD, XRPUSD ]

- **Just2Trade** - [ XRPUSD*, BTCUSD, ETHUSD]

**Note:** the '*'-ed tickers are supported by the broker, it is possible to obtain their ticks however the broker disabled the trading for these tickers