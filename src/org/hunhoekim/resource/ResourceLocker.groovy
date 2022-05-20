package org.hunhoekim.resource

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import groovy.transform.stc.SecondParam
import groovy.transform.stc.ClosureParams
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

/* groovylint-disable-next-line ClassJavadoc */
@CompileDynamic
class ResourceLocker implements Serializable {

  @CompileStatic
  class Resource implements Serializable {

    private static final long serialVersionUID = 1
    String label
    String name

  }

  @CompileStatic
  class Timeout implements Serializable {

    private static final long serialVersionUID = 1
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

    this.script.parallel(
      'ResourceLocker.AcquireStep': {
        this.lockRecursive(resourceLabels, [], onAcquire)
      },
      'ResourceLocker.TimeoutStep': {
        try {
          this.script.timeout(time: timeout.time, unit: timeout.unit) {
            /* groovylint-disable-next-line EmptyWhileStatement */
            while (!this.isAcquired) { /* do nothing */ }
          }
        } catch (FlowInterruptedException e) {
          if (!this.isAcquired) {
            throw e
          }
        }
      },
      failFast: true
    )
  }

  private void lockRecursive(
    List<String> remainResourceLabels,
    List<Resource> resources,
    @ClosureParams(SecondParam) Closure onAcquire) {
    if (!remainResourceLabels) {
      this.isAcquired = true
      onAcquire(resources)
      return
    }
    String resourceLabel = remainResourceLabels.head()
    this.script.lock(label: resourceLabel, variable: 'LOCKED_RESOURCE', quantity: 1) {
      resources.add(new Resource(label:resourceLabel, name: this.script.env.LOCKED_RESOURCE))
      this.lockRecursive(remainResourceLabels.tail(), resources, onAcquire)
    }
  }

}
