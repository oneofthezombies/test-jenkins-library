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
    static final Integer DEFAULT_RETRY_COUNT = 3

    final Integer time
    final String unit
    final Integer retryCount
    private final CpsScript script

    Timeout(CpsScript script, Map args) {
      this.script = script
      this.time = args.get('time', DEFAULT_TIME) as Integer
      this.unit = args.get('unit', DEFAULT_UNIT)
      this.retryCount = args.get('retryCount', DEFAULT_RETRY_COUNT) as Integer
    }

  }

  class TimeoutException extends Exception {

    FlowInterruptedException original

  }

  private static final long serialVersionUID = 1
  private final CpsScript script
  private Boolean isAcquired = false

  ResourceLocker(CpsScript script) {
    this.script = script
  }

  void lock(Map args) {
    List<String> resourceLabels = args['resourceLabels']
    Closure onAcquire = args['onAcquire']
    Timeout timeout = new Timeout(this.script, args.get('timeout', [:]))

    TimeoutException lastTimeoutException = null
    for (Integer i = 0; i < timeout.retryCount; ++i) {
      try {
        this.script.parallel(
          'ResourceLocker acquire step': {
            this.lockRecursive(resourceLabels, [], onAcquire)
          },
          'ResourceLocker timeout step': {
            try {
              this.script.timeout(time: timeout.time, unit: timeout.unit) {
                /* groovylint-disable-next-line EmptyWhileStatement, NestedBlockDepth */
                while (!this.isAcquired) { /* do nothing */ }
              }
            } catch (FlowInterruptedException e) {
              if (!this.isAcquired) {
                throw new TimeoutException(original: e)
              }
            }
          },
          failFast: true
        )
        return
      } catch (TimeoutException e) {
        lastTimeoutException = e
      }
    }
    throw lastTimeoutException.original
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
