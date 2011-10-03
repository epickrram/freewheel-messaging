package com.epickrram.junit;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class PerfTestRunner extends BlockJUnit4ClassRunner
{
    private final List<FrameworkMethod> methods;
    private final Map<String, PerfResult> perfResults = new TreeMap<String, PerfResult>();

	public PerfTestRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
        methods = getCandidateTestMethods(getChildren());
    }

    @Override
    protected Statement childrenInvoker(final RunNotifier notifier)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                for (FrameworkMethod frameworkMethod : methods)
                {
                    final PerfTest perfTest = frameworkMethod.getMethod().getAnnotation(PerfTest.class);
                    for(int i = 0; i < perfTest.warmUpRuns(); i++)
                    {
                        System.err.println("Running warmup " + (i + 1));
                        runChild(frameworkMethod, new NonFinishingRunNotifier(notifier));
                    }

                    final long startNanos = System.nanoTime();
                    runChild(frameworkMethod, new NonStartingRunNotifier(notifier));
                    final long endNanos = System.nanoTime();
                    final long durationNanos = endNanos - startNanos;
                    final double seconds = durationNanos / (double) 1000000000;
                    System.err.println("Test complete for " + perfTest.name());
                    final long opsPerSecond = new BigDecimal(((perfTest.iterations()) / seconds)).setScale(0, BigDecimal.ROUND_FLOOR).longValue();
                    System.err.println("Performed " + opsPerSecond + " ops per second");
                    perfResults.put(perfTest.name().replace(' ', '_'), new PerfResult(perfTest.name(), opsPerSecond));
                }
            }
        };
    }

    @Override
    public void run(final RunNotifier notifier)
    {
        super.run(notifier);

        final File resultsFile = new File("/tmp/" + getTestClass().getJavaClass().getSimpleName() + "-perf-results.csv");
        boolean writeHeader = false;
        if(!resultsFile.exists())
        {
            resultsFile.getParentFile().mkdirs();
            writeHeader = true;
        }
        try
        {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile, true));
            if(writeHeader)
            {
                final Set<String> testNames = perfResults.keySet();
                int c = 0;
                for (String name : testNames)
                {
                    if(c != 0)
                    {
                        writer.append(" ");
                    }
                    writer.append('\"').append(name).append('\"');
                    c++;
                }
                writer.newLine();
            }
            writer.append(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ");

            final Set<Map.Entry<String, PerfResult>> entries = perfResults.entrySet();
            int c = 0;
            for (Map.Entry<String, PerfResult> entry : entries)
            {
                if(c != 0)
                {
                    writer.append(" ");
                }
                writer.append(Long.toString(entry.getValue().opsPerSecond));
                c++;
            }
            writer.newLine();
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to write perf results file", e);
        }
    }

    private static final class PerfResult
    {
        private final String name;
        private final long opsPerSecond;

        private PerfResult(final String name, final long opsPerSecond)
        {
            this.name = name;
            this.opsPerSecond = opsPerSecond;
        }
    }

    private static final class NonStartingRunNotifier extends RunNotifier
    {
        private final RunNotifier delegate;

        public NonStartingRunNotifier(final RunNotifier delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void addListener(final RunListener listener)
        {
            delegate.addListener(listener);
        }

        @Override
        public void removeListener(final RunListener listener)
        {
            delegate.removeListener(listener);
        }

        @Override
        public void fireTestRunStarted(final Description description)
        {
            // no-op
        }

        @Override
        public void fireTestRunFinished(final Result result)
        {
            delegate.fireTestRunFinished(result);
        }

        @Override
        public void fireTestStarted(final Description description)
                throws StoppedByUserException
        {
            // no-op
        }

        @Override
        public void fireTestFailure(final Failure failure)
        {
            delegate.fireTestFailure(failure);
        }

        @Override
        public void fireTestAssumptionFailed(final Failure failure)
        {
            delegate.fireTestAssumptionFailed(failure);
        }

        @Override
        public void fireTestIgnored(final Description description)
        {
            delegate.fireTestIgnored(description);
        }

        @Override
        public void fireTestFinished(final Description description)
        {
            delegate.fireTestFinished(description);
        }

        @Override
        public void pleaseStop()
        {
            delegate.pleaseStop();
        }

        @Override
        public void addFirstListener(final RunListener listener)
        {
            delegate.addFirstListener(listener);
        }
    }

    private static final class NonFinishingRunNotifier extends RunNotifier
    {
        private final RunNotifier delegate;

        public NonFinishingRunNotifier(final RunNotifier delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void addListener(final RunListener listener)
        {
            delegate.addListener(listener);
        }

        @Override
        public void removeListener(final RunListener listener)
        {
            delegate.removeListener(listener);
        }

        @Override
        public void fireTestRunStarted(final Description description)
        {
            delegate.fireTestRunStarted(description);
        }

        @Override
        public void fireTestRunFinished(final Result result)
        {
            // no-op
        }

        @Override
        public void fireTestStarted(final Description description)
                throws StoppedByUserException
        {
            delegate.fireTestStarted(description);
        }

        @Override
        public void fireTestFailure(final Failure failure)
        {
            delegate.fireTestFailure(failure);
        }

        @Override
        public void fireTestAssumptionFailed(final Failure failure)
        {
            delegate.fireTestAssumptionFailed(failure);
        }

        @Override
        public void fireTestIgnored(final Description description)
        {
            delegate.fireTestIgnored(description);
        }

        @Override
        public void fireTestFinished(final Description description)
        {
            // no-op
        }

        @Override
        public void pleaseStop()
        {
            delegate.pleaseStop();
        }

        @Override
        public void addFirstListener(final RunListener listener)
        {
            delegate.addFirstListener(listener);
        }
    }

    private List<FrameworkMethod> getCandidateTestMethods(final List<FrameworkMethod> frameworkMethods)
    {
        final List<FrameworkMethod> candidateMethods = new ArrayList<FrameworkMethod>();
        for (final FrameworkMethod frameworkMethod : frameworkMethods)
        {
            if(shouldRunMethod(frameworkMethod))
            {
                candidateMethods.add(frameworkMethod);
            }
        }
        return candidateMethods;
    }

    private boolean shouldRunMethod(final FrameworkMethod frameworkMethod)
    {
        return frameworkMethod.getMethod().getAnnotation(PerfTest.class) != null;
    }
}