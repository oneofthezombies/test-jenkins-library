package org.hunhoekim.resource

import org.jenkinsci.plugins.workflow.cps.CpsScript

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
  private final CpsScript script
  private Boolean isAcquired = false
  Integer retryCount = 10

  ResourceLocker(CpsScript script) {
    this.script = script
  }

  void lock(Map args) {
    List<String> resourceLabels = args['resourceLabels']
    Closure onAcquire = args['onAcquire']
    Timeout timeout = args.get('timeout', new Timeout(time: Timeout.DEFAULT_TIME, unit: Timeout.DEFAULT_UNIT))
    this.script.echo "1 ${args}"
    this.script.echo "2 ${timeout}"

    this.script.parallel(
      'ResourceLocker.AcquireStep': {
        this.lockRecursive(resourceLabels, [], onAcquire)
      },
      'ResourceLocker.TimeoutStep': {
        try {
          this.script.timeout(time: timeout.time, unit: timeout.unit) {
            while (true) { /* do nothing */ }
          }
        } catch (Exception e) {
          if (!this.isAcquired) {
            throw e
          }
        }
      },
      failFast: true
    )
  }

  private void lockRecursive(List<String> remainResourceLabels, List<Resource> resources, Closure onAcquire) {
    if (!remainResourceLabels) {
      isAcquired = true
      onAcquire(resources)
    }
    String resourceLabel = remainResourceLabels.head()
    this.script.lock(label: resourceLabel, variable: 'LOCKED_RESOURCE') {
      resources.add(new Resource(label:resourceLabel, name: this.script.env.LOCKED_RESOURCE))
      lockRecursive(remainResourceLabels.tail(), resources, onAcquire)
    }
  }

}
