class ResourceLocker implements Serializable {

  class Resource {

    String label
    String name

  }

  class Timeout {

    static final String UNIT_HOURS = 'HOURS'
    static final String UNIT_MINUTES = 'MINUTES'
    static final String UNIT_SECONDS = 'SECONDS'
    static final Integer DEFAULT_TIME = 10
    static final String DEFAULT_UNIT = UNIT_SECONDS
    Integer time
    String unit

  }

  private static final long serialVersionUID = 1
  private Boolean isAcquired = false
  Integer retryCount = 10

  void lock(Map args) {
    List<String> resourceLabels = args['resourceLabels']
    Closure onAcquire = args['onAcquire']
    Timeout timeout = args.get('timeout', new Timeout(time: Timeout.DEFAULT_TIME, unit: Timeout.DEFAULT_UNIT))

    parallel(
      'ResourceLocker.AcquireStep': {
        this.lockRecursive(resourceLabels, [], onAcquire)
      },
      'ResourceLocker.TimeoutStep': {
        try {
          timeout(time: timeout.time, unit: timeout.unit) { /* do nothing */ }
        } catch (Exception e) {
          if (!this.isAcquired) {
            throw e
          }
        }
      },
      fastFail: true
    )
  }

  private void lockRecursive(List<String> remainResourceLabels, List<Resource> resources, Closure onAcquire) {
    if (!remainResourceLabels) {
      isAcquired = true
      onAcquire(resources)
    }
    String resourceLabel = remainResourceLabels.head()
    lock(label: resourceLabel, variable: 'LOCKED_RESOURCE') {
      resources.add(new Resource(label:resourceLabel, name: env.LOCKED_RESOURCE))
      lockRecursive(remainResourceLabels.tail(), resources, onAcquire)
    }
  }

}
