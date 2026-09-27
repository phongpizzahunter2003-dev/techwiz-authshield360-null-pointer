import { useEffect, useRef, useState } from 'react'

/**
 * Countdown timer in seconds. Used for OTP validity and resend cooldown (UC-02/04).
 * Returns { seconds, active, restart } and formats as mm:ss when needed.
 */
export function useCountdown(initialSeconds = 0, onDone) {
  const [seconds, setSeconds] = useState(initialSeconds)
  const onDoneRef = useRef(onDone)
  onDoneRef.current = onDone

  useEffect(() => {
    if (seconds <= 0) return undefined
    const id = setInterval(() => {
      setSeconds((s) => {
        if (s <= 1) {
          clearInterval(id)
          onDoneRef.current?.()
          return 0
        }
        return s - 1
      })
    }, 1000)
    return () => clearInterval(id)
  }, [seconds > 0]) // eslint-disable-line react-hooks/exhaustive-deps

  return {
    seconds,
    active: seconds > 0,
    restart: (value) => setSeconds(value),
    label: format(seconds),
  }
}

export function format(totalSeconds) {
  const safe = Math.max(0, Math.floor(totalSeconds || 0))
  const m = String(Math.floor(safe / 60)).padStart(2, '0')
  const s = String(safe % 60).padStart(2, '0')
  return `${m}:${s}`
}
