const { assertBackendReady, resetBaselineConfig } = require('./api')

/**
 * Runs once before the suite: proves the backend is reachable and seeded, then restores the
 * documented S1 baseline so a run is repeatable even if a previous run changed the mode.
 */
module.exports = async () => {
  await assertBackendReady()
  await resetBaselineConfig()
  // eslint-disable-next-line no-console
  console.log('[e2e] backend reachable — auth configuration reset to the S1 baseline')
}
