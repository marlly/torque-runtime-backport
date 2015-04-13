# Torque Runtime 3 Backport

This version is based on legacy Apache Torque Runtime 3.3 release with some additional fixies and new funcionality backported from version [4.0][torque-4.0].

## Fixies
* [TORQUE-108][] - Criteria addJoin causes incorrect SQL to be generated when optional schema references are in use (Oracle)

## New funcionality

### `TransactionManager`

Custom transaction manager is necessary when you need to control database transactions using external framework such as Spring. To specify alternative implementation of `TransactionManager` interface, you can choose one of the following options. Otherwise default `TransactionManagerImpl` will be used.

#### Configuration approach

Edit `Torque.properties` and add next line:

```ini
torque.transactionManager = package.YourTxManagerImplementationClass
```

Note that your implementation must have default (parameter-less) constructor.

#### Programmatic approach

```java
Torque.init(conf);
Transaction.setTransactionManager(yourTxManagerImplementationInstance);
```

For more info please see original [documentation][txhandling] about transaction handling in Torque 4.0

## References

If you are using Spring framework and you want to take advantage of its great transaction management along with Torque, please look at my [spring-torque-tx][] project.

[torque-4.0]: http://db.apache.org/torque/torque-4.0
[TORQUE-108]: https://issues.apache.org/jira/browse/TORQUE-108
[txhandling]: http://db.apache.org/torque/torque-4.0/documentation/orm-reference/connections-transactions.html
[spring-torque-tx]: https://github.com/marlly/spring-torque-tx 
