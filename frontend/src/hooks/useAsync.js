import { useEffect, useState } from 'react'

/** Small data-fetching hook with loading/error state for list pages. */
export function useAsync(fn, deps = [], options = {}) {
  const { immediate = true } = options
  const [state, setState] = useState({ data: null, loading: immediate, error: null })

  const run = async () => {
    setState((s) => ({ ...s, loading: true, error: null }))
    try {
      const result = await fn()
      setState({ data: result, loading: false, error: null })
      return result
    } catch (error) {
      setState({ data: null, loading: false, error })
      return null
    }
  }

  useEffect(() => {
    if (immediate) run()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { ...state, reload: run, setData: (d) => setState((s) => ({ ...s, data: d })) }
}
