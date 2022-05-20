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

    class Exception extends FlowInterruptedException {

    }

    private static final long serialVersionUID = 1
    static final String UNIT_HOURS = 'HOURS'
    static final String UNIT_MINUTES = 'MINUTES'
    static final String UNIT_SECONDS = 'SECONDS'
    static final Integer DEFAULT_TIME = 10
    static final String DEFAULT_UNIT = UNIT_SECONDS
    static final Integer DEFAULT_RETRY_COUNT = 3

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
    Map timeout = args.get('timeout', [:])
    Integer timeoutTime = timeout.get('time', Timeout.DEFAULT_TIME)
    String timeoutUnit = timeout.get('unit', Timeout.DEFAULT_UNIT)
    Integer timeoutRetryCount = timeout.get('retryCount', Timeout.DEFAULT_RETRY_COUNT)

    Timeout.Exception lastTimeoutException = null
    for (Integer i = 0; i < timeoutRetryCount; ++i) {
      try {
        this.script.parallel(
          'ResourceLocker acquire step': {
            this.lockRecursive(resourceLabels, [], onAcquire)
          },
          'ResourceLocker timeout step': {
            try {
              this.script.timeout(time: timeoutTime, unit: timeoutUnit) {
                /* groovylint-disable-next-line EmptyWhileStatement, NestedBlockDepth */
                while (!this.isAcquired) { /* do nothing */ }
              }
            } catch (FlowInterruptedException e) {
              if (!this.isAcquired) {
                throw new Timeout.Exception(e)
              }
            }
          },
          failFast: true
        )
        return
      /* groovylint-disable-next-line CatchException, EmptyCatchBlock */
      } catch (Timeout.Exception e) {
        lastTimeoutException = e
      }
    }
    throw lastTimeoutException
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
