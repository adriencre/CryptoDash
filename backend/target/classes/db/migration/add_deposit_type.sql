-- Migration : Ajout du type DEPOSIT à la colonne 'type' de la table transactions
-- À exécuter UNE SEULE FOIS si la base de données a été créée avant l'ajout du type DEPOSIT

ALTER TABLE transactions 
  MODIFY COLUMN type ENUM('BUY','SELL','SEND','RECEIVE','DEPOSIT') COLLATE utf8mb4_unicode_ci NOT NULL;
