import {
  AlertTriangle,
  ChefHat,
  Clock3,
  Heart,
  HeartOff,
  UsersRound,
} from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import type { RecipeCatalogItem } from '@/features/recipes/api/recipe-catalog'
import { backendUrl } from '@/lib/backend-url'

interface RecipeCardProps {
  recipe: RecipeCatalogItem
  favoriteActionPending?: boolean
  onRemoveFavorite?: (recipe: RecipeCatalogItem) => void
}

export function RecipeCard({
  recipe,
  favoriteActionPending = false,
  onRemoveFavorite,
}: RecipeCardProps) {
  const detailsUrl = `/recipes/${recipe.id}`

  return (
    <Card className="overflow-hidden bg-background/90 py-0 shadow-sm transition-transform duration-200 hover:-translate-y-1">
      <Link className="block" to={detailsUrl}>
        <div className="relative aspect-[4/3] overflow-hidden bg-secondary">
          {recipe.coverUrl ? (
            <img
              alt={`Обложка блюда «${recipe.name}»`}
              className="size-full object-cover transition-transform duration-300 hover:scale-[1.03]"
              loading="lazy"
              src={backendUrl(recipe.coverUrl)}
            />
          ) : (
            <div className="grid size-full place-items-center text-primary">
              <ChefHat aria-hidden="true" className="size-12" />
            </div>
          )}
          {recipe.favorite && (
            <Badge className="absolute top-3 right-3 gap-1">
              <Heart aria-hidden="true" className="size-3.5 fill-current" />
              Избранное
            </Badge>
          )}
        </div>
      </Link>

      <CardHeader className="gap-3 pt-5">
        <div className="flex flex-wrap gap-2">
          <Badge variant="secondary">{recipe.cuisine}</Badge>
          <Badge variant="outline">{recipe.status}</Badge>
        </div>
        <div>
          <h3 className="font-heading text-xl leading-tight font-semibold">
            <Link className="hover:text-primary" to={detailsUrl}>
              {recipe.name}
            </Link>
          </h3>
          <p className="mt-1 text-sm text-muted-foreground">{recipe.englishName}</p>
        </div>
      </CardHeader>

      <CardContent className="pb-5">
        <p className="line-clamp-3 min-h-18 text-sm leading-6 text-muted-foreground">
          {recipe.description}
        </p>
        <div className="mt-5 flex flex-wrap gap-x-4 gap-y-2 text-sm font-medium">
          <span className="flex items-center gap-1.5">
            <Clock3 aria-hidden="true" className="size-4 text-primary" />
            {recipe.totalMinutes} мин
          </span>
          <span className="flex items-center gap-1.5">
            <UsersRound aria-hidden="true" className="size-4 text-primary" />
            {recipe.servings} порц.
          </span>
          <span>{recipe.difficulty}</span>
        </div>

        {recipe.restrictionWarnings.length > 0 && (
          <div className="mt-4 rounded-xl border border-amber-500/30 bg-amber-50 p-3 text-sm text-amber-950">
            <p className="flex items-center gap-2 font-semibold">
              <AlertTriangle aria-hidden="true" className="size-4" />
              Есть ограничения
            </p>
            <ul className="mt-2 space-y-1">
              {recipe.restrictionWarnings.map((warning) => (
                <li key={`${warning.ingredientName}-${warning.modeName}`}>
                  {warning.ingredientName}: {warning.modeName}
                </li>
              ))}
            </ul>
          </div>
        )}

        {onRemoveFavorite && (
          <Button
            className="mt-5 w-full"
            disabled={favoriteActionPending}
            onClick={() => onRemoveFavorite(recipe)}
            type="button"
            variant="outline"
          >
            <HeartOff aria-hidden="true" data-icon="inline-start" />
            {favoriteActionPending ? 'Удаляем...' : 'Удалить из избранного'}
          </Button>
        )}
      </CardContent>
    </Card>
  )
}
