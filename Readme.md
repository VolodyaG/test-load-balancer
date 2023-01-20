### "Kind of" Load Balancer
The requirement for the assignment is to write a code that "kind of" simulates what a real load balancer is doing.

The goal of a project is not to make the most powerful solution,
but show the possibility to design something that works now, have a simple design with the possibility for extension in future

**Stack**
* Kotlin
* Gradle
* Kotest for testing

**Domain:**
* Load Balancer - distributes incoming requests to providers 
* Provider - destination of a request

**Features:**
1. Load balancing strategies (Random, Round-robin)
2. Add or remove providers from Load Balancer in runtime
3. Rate limiting
4. Health Checks

### Design Notes
* **LoadBalancer** - simple class whole responsibility of which is to route requests to some providers from provider registry
* **ProvidersRegistry** - Abstract class that encapsulates all the complexity for managing providers, such as picking next healthy provider or managing concurrent access to the pool of providers
  * **RandomProvidersRegistry** and **RoundRobbinProvidersRegistry** are implementations of different picking strategies overriding template method `pickNext()`
* **ProviderWithHealthcheck** - is a decorator, that implements and wraps **Provider** interface. 
Encapsulates logic for handling health checks and providing only one public boolean property `isHealhy`

#### Room for improvement
1. Perhaps it is possible to reduce amount of locking for better concurency
2. Stop health checks when providers are removed
3. Rethink healthcheck interface for easier testing
4. Remove dependency on `delay()` and `until()` in tests. 
E.g. use coroutines test [advanceTimeBy](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/advance-time-by.html)
or again rethink interfaces