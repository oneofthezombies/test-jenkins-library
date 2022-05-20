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
      /* groovylint-disable-next-line NoDef, VariableTypeRequired */
      def time = args.get('time', DEFAULT_TIME)
      /* groovylint-disable-next-line Instanceof */
      if (!(time instanceof Integer)) {
        time = time.toInteger()
        this.script.println "@@@"
      }
      String unit = args.get('unit', DEFAULT_UNIT)
      /* groovylint-disable-next-line NoDef, VariableTypeRequired */
      def retryCount = args.get('retryCount', DEFAULT_RETRY_COUNT)
      this.script.println "@@@@ ${retryCount}"
      /* groovylint-disable-next-line Instanceof */
      if (!(retryCount instanceof Integer)) {
        retryCount = retryCount.toInteger()
        this.script.println "@@@"
      }
      this.time = time
      this.unit = unit
      this.retryCount = retryCount

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
    this.script.println "lock(resourceLabels: ${resourceLabels}, timeout:{time: ${timeout.time}, unit: ${timeout.unit}, retryCount: ${timeout.retryCount}}"

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
