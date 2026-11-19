import { ChefHat } from 'lucide-react'
import { Link, Outlet } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'

export function ApplicationShell() {
  return (
    <div className="min-h-svh overflow-hidden">
      <header className="border-b border-border/70 bg-background/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-4 sm:px-8">
          <Link
            className="flex items-center gap-3 font-heading text-base font-semibold tracking-tight"
            to="/"
          >
            <span className="grid size-10 place-items-center rounded-2xl bg-primary text-primary-foreground shadow-sm">
              <ChefHat aria-hidden="true" className="size-5" />
            </span>
            Что приготовить?
          </Link>
          <Badge variant="secondary">Этап 9.5</Badge>
        </div>
      </header>

      <Outlet />

      <footer className="mx-auto flex w-full max-w-6xl flex-col gap-2 px-5 py-8 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-8">
        <p>Что приготовить? · React-клиент</p>
        <p>Бизнес-логика остаётся в Spring Boot</p>
      </footer>
    </div>
  )
}
