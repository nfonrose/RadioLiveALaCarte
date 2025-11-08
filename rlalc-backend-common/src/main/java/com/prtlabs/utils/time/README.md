
# Time handling in PRTLabs applications


## Use dependency injected time

All the code should use

    @Inject IPrtTimeProviderService timeProviderService;

and not try to use Instant.now() or PrtTimeHelper.getCurrentTimeEpochSeconds() so that tests
can play on time and check corner cases.
