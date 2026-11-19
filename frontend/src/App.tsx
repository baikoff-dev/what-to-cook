import {
  ArrowUpRight,
  Check,
  MonitorSmartphone,
  Server,
  Sparkles,
} from 'lucide-react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { ApplicationShell } from '@/components/layout/application-shell'
import { FavoriteRecipesPage } from '@/features/favorites/components/favorite-recipes-page'
import { RecipeCatalogSection } from '@/features/recipes/components/recipe-catalog-section'
import { RecipeDetailsPage } from '@/features/recipes/components/recipe-details-page'
import { backendUrl } from '@/lib/backend-url'

const foundations = [
  {
    icon: MonitorSmartphone,
    title: 'Самостоятельный frontend',
    description:
      'React, TypeScript и Vite развиваются отдельно от серверных HTML-шаблонов.',
  },
  {
    icon: Server,
    title: 'Единая граница API',
    description:
      'Запросы по адресу /api во время разработки проксируются в Spring Boot.',
  },
  {
    icon: Sparkles,
    title: 'Управляемый интерфейс',
    description:
      'Компоненты shadcn/ui находятся в проекте и могут меняться вместе с продуктом.',
  },
]

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<ApplicationShell />}>
          <Route index element={<HomePage />} />
          <Route path="recipes/favorites" element={<FavoriteRecipesPage />} />
          <Route path="recipes/:recipeId" element={<RecipeDetailsPage />} />
          <Route path="*" element={<Navigate replace to="/" />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

function HomePage() {
  return (
    <>
      <main>
        <section className="relative">
          <div className="pointer-events-none absolute -top-40 right-[-12rem] size-[30rem] rounded-full bg-accent/65 blur-3xl" />
          <div className="pointer-events-none absolute bottom-0 left-[-8rem] size-72 rounded-full bg-primary/10 blur-3xl" />

          <div className="relative mx-auto grid max-w-6xl gap-12 px-5 py-16 sm:px-8 sm:py-24 lg:grid-cols-[1.15fr_0.85fr] lg:items-center lg:py-28">
            <div className="max-w-3xl">
              <Badge className="mb-6" variant="outline">
                Новый клиент приложения
              </Badge>
              <h1 className="font-heading text-5xl leading-[0.98] font-semibold tracking-[-0.055em] text-balance sm:text-6xl lg:text-7xl">
                Выбирай блюдо,
                <span className="text-primary"> а не компромисс</span>
              </h1>
              <p className="mt-7 max-w-2xl text-lg leading-8 text-muted-foreground sm:text-xl">
                React получает реальные данные Spring Boot и помогает искать
                рецепты по времени, кухне и способу приготовления.
              </p>
              <div className="mt-9 flex flex-col gap-3 sm:flex-row">
                <Button asChild size="lg">
                  <a href="#recipes">
                    Смотреть рецепты
                    <ArrowUpRight aria-hidden="true" data-icon="inline-end" />
                  </a>
                </Button>
                <Button asChild size="lg" variant="outline">
                  <a href={backendUrl('/')}>Открыть прежнюю версию</a>
                </Button>
              </div>
            </div>

            <Card className="relative overflow-hidden border-primary/15 bg-card/90 shadow-xl shadow-primary/5">
              <div className="h-2 bg-[linear-gradient(90deg,var(--primary),var(--chart-2),var(--accent-foreground))]" />
              <CardHeader className="pb-3">
                <div className="mb-4 flex items-center justify-between">
                  <Badge variant="secondary">Frontend</Badge>
                  <span className="flex items-center gap-2 text-sm text-muted-foreground">
                    <span className="size-2 rounded-full bg-emerald-600" />
                    Готов к разработке
                  </span>
                </div>
                <CardTitle className="text-2xl">Технический фундамент</CardTitle>
                <CardDescription className="text-base leading-7">
                  Независимая сборка интерфейса без изменения работающей версии
                  приложения.
                </CardDescription>
              </CardHeader>
              <CardContent className="grid gap-3">
                {['React + TypeScript', 'Vite + Tailwind CSS', 'shadcn/ui + Vitest'].map(
                  (technology) => (
                    <div
                      className="flex items-center gap-3 rounded-xl border bg-background/70 px-4 py-3 text-sm font-medium"
                      key={technology}
                    >
                      <span className="grid size-7 place-items-center rounded-full bg-accent text-accent-foreground">
                        <Check aria-hidden="true" className="size-4" />
                      </span>
                      {technology}
                    </div>
                  ),
                )}
              </CardContent>
            </Card>
          </div>
        </section>

        <RecipeCatalogSection />

        <section
          className="border-y border-border/70 bg-card/55"
          id="foundation"
        >
          <div className="mx-auto max-w-6xl px-5 py-14 sm:px-8 sm:py-20">
            <div className="mb-9 max-w-2xl">
              <p className="mb-3 text-sm font-semibold tracking-[0.16em] text-primary uppercase">
                Архитектурная граница
              </p>
              <h2 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
                Новый интерфейс, прежняя бизнес-логика
              </h2>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              {foundations.map(({ icon: Icon, title, description }) => (
                <Card className="bg-background/80" key={title}>
                  <CardHeader>
                    <span className="mb-3 grid size-11 place-items-center rounded-2xl bg-secondary text-primary">
                      <Icon aria-hidden="true" className="size-5" />
                    </span>
                    <CardTitle>{title}</CardTitle>
                    <CardDescription className="leading-6">
                      {description}
                    </CardDescription>
                  </CardHeader>
                </Card>
              ))}
            </div>
          </div>
        </section>
      </main>
    </>
  )
}

export default App
