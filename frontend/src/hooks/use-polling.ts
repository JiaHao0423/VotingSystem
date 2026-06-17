import { useEffect, useRef } from 'react'

const DEFAULT_INTERVAL_MS = 5000

export function usePolling(callback: () => void | Promise<void>, intervalMs = DEFAULT_INTERVAL_MS, enabled = true) {
  const savedCallback = useRef(callback)

  useEffect(() => {
    savedCallback.current = callback
  }, [callback])

  useEffect(() => {
    if (!enabled) return

    const tick = () => {
      void savedCallback.current()
    }

    const id = window.setInterval(tick, intervalMs)
    return () => window.clearInterval(id)
  }, [enabled, intervalMs])
}
